package com.example.truyen.service.impl;

import com.example.truyen.dto.request.CreatePaymentOrderRequest;
import com.example.truyen.dto.response.PaymentOrderResponse;
import com.example.truyen.entity.CoinPackage;
import com.example.truyen.entity.PaymentOrder;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.CoinPackageRepository;
import com.example.truyen.repository.PaymentOrderRepository;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.service.PaymentService;
import com.example.truyen.service.VNPayService;
import com.example.truyen.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final CoinPackageRepository coinPackageRepository;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;
    private final WalletService walletService;

    @Transactional
    @Override
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request, String ipAddress) {
        User user = getCurrentUser();

        CoinPackage pkg = coinPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("CoinPackage", "id", request.getPackageId()));

        if (!pkg.getIsActive()) {
            throw new BadRequestException("This package is no longer available");
        }

        String orderCode = "TRU" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        int totalCoins = pkg.getCoins() + pkg.getBonusCoins();

        PaymentOrder order = PaymentOrder.builder()
                .orderCode(orderCode)
                .user(user)
                .coinPackage(pkg)
                .amount(pkg.getPrice())
                .coinsToAdd(totalCoins)
                .status(PaymentOrder.Status.PENDING)
                .build();

        order = paymentOrderRepository.save(order);

        String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
        order.setVnpTxnRef(orderCode);
        paymentOrderRepository.save(order);

        return toResponse(order, paymentUrl);
    }

    @Transactional
    @Override
    public String handleVNPayReturn(Map<String, String> params) {
        if (!vnPayService.verifyCallback(params)) {
            return "INVALID_SIGNATURE";
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");

        PaymentOrder order = paymentOrderRepository.findByOrderCode(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "orderCode", txnRef));

        if (order.getStatus() != PaymentOrder.Status.PENDING) {
            return "ALREADY_PROCESSED";
        }

        if (paymentOrderRepository.existsByVnpTransactionNo(transactionNo)) {
            return "DUPLICATE_TRANSACTION";
        }

        order.setVnpTransactionNo(transactionNo);

        if ("00".equals(responseCode)) {
            order.setStatus(PaymentOrder.Status.SUCCESS);
            order.setCompletedAt(LocalDateTime.now());
            paymentOrderRepository.save(order);

            walletService.addCoins(
                    order.getUser().getId(),
                    order.getCoinsToAdd(),
                    "Nap xu - don hang " + order.getOrderCode(),
                    order.getId());
            return "SUCCESS";
        } else {
            order.setStatus(PaymentOrder.Status.FAILED);
            paymentOrderRepository.save(order);
            return "FAILED";
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentOrderResponse> getMyOrders(Pageable pageable) {
        User user = getCurrentUser();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(o -> toResponse(o, null));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentOrderResponse> getAllOrders(Pageable pageable) {
        return paymentOrderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(o -> toResponse(o, null));
    }

    /**
     * IPN handler: VNPay gọi server-to-server ngay sau khi thanh toán xong.
     * Phải trả về đúng định dạng JSON: {"RspCode": "00", "Message": "OK"}
     */
    @Transactional
    @Override
    public String handleVNPayIPN(Map<String, String> params) {
        if (!vnPayService.verifyCallback(params)) {
            log.warn("IPN: invalid signature");
            return "97"; // Invalid signature
        }

        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");
        String responseCode = params.get("vnp_ResponseCode");

        PaymentOrder order = paymentOrderRepository.findByOrderCode(txnRef).orElse(null);
        if (order == null) {
            log.warn("IPN: order not found for txnRef={}", txnRef);
            return "01"; // Order not found
        }

        if (order.getStatus() != PaymentOrder.Status.PENDING) {
            log.info("IPN: order {} already processed (status={})", txnRef, order.getStatus());
            return "02"; // Already confirmed
        }

        if (paymentOrderRepository.existsByVnpTransactionNo(transactionNo)) {
            log.warn("IPN: duplicate transaction {}", transactionNo);
            return "02"; // Duplicate
        }

        order.setVnpTransactionNo(transactionNo);

        if ("00".equals(responseCode)) {
            order.setStatus(PaymentOrder.Status.SUCCESS);
            order.setCompletedAt(LocalDateTime.now());
            paymentOrderRepository.save(order);
            walletService.addCoins(
                    order.getUser().getId(),
                    order.getCoinsToAdd(),
                    "Nap xu - don hang " + order.getOrderCode(),
                    order.getId());
            log.info("IPN: order {} SUCCESS, added {} coins to user {}",
                    txnRef, order.getCoinsToAdd(), order.getUser().getId());
            return "00";
        } else {
            order.setStatus(PaymentOrder.Status.FAILED);
            paymentOrderRepository.save(order);
            log.info("IPN: order {} FAILED, responseCode={}", txnRef, responseCode);
            return "00"; // VNPay yêu cầu luôn trả 00 dù thành công hay thất bại
        }
    }

    /**
     * Scheduled: chạy mỗi phút, hủy hàng loạt đơn PENDING quá 15 phút.
     * Dùng bulk UPDATE thay vì load từng entity.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    @Override
    public void cancelExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        int cancelled = paymentOrderRepository.cancelPendingOrdersBefore(cutoff);
        if (cancelled > 0) {
            log.info("Cancelled {} expired PENDING orders (older than 15 minutes)", cancelled);
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PaymentOrderResponse toResponse(PaymentOrder order, String paymentUrl) {
        return PaymentOrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .packageName(order.getCoinPackage().getName())
                .amount(order.getAmount())
                .coinsToAdd(order.getCoinsToAdd())
                .status(order.getStatus())
                .paymentUrl(paymentUrl)
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }
}
