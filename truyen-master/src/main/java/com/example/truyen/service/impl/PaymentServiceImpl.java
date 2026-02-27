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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
