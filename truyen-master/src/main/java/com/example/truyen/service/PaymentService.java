package com.example.truyen.service;

import com.example.truyen.dto.request.CreatePaymentOrderRequest;
import com.example.truyen.dto.response.PaymentOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface PaymentService {

    PaymentOrderResponse createOrder(CreatePaymentOrderRequest request, String ipAddress);

    String handleVNPayReturn(Map<String, String> params);

    // IPN: VNPay server gọi trực tiếp (server-to-server, không qua browser)
    String handleVNPayIPN(Map<String, String> params);

    // Scheduled: hủy đơn PENDING quá 15 phút
    void cancelExpiredOrders();

    Page<PaymentOrderResponse> getMyOrders(Pageable pageable);

    Page<PaymentOrderResponse> getAllOrders(Pageable pageable);
}
