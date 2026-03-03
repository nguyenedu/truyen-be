package com.example.truyen.controller;

import com.example.truyen.dto.request.CreatePaymentOrderRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.PaymentOrderResponse;
import com.example.truyen.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${vnpay.frontend-return-url}")
    private String frontendReturnUrl;

    // Tạo đơn nạp xu (yêu cầu đăng nhập)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        PaymentOrderResponse order = paymentService.createOrder(request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create payment order successfully", order));
    }

    // VNPay callback - redirect về frontend
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        String result = paymentService.handleVNPayReturn(params);
        String orderCode = params.getOrDefault("vnp_TxnRef", "");

        String status = "SUCCESS".equals(result) ? "success" : "failed";
        String redirectUrl = frontendReturnUrl + "?status=" + status + "&orderCode=" + orderCode;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // VNPay IPN - server-to-server, không qua browser
    // VNPay yêu cầu trả về JSON: {"RspCode": "xx", "Message": "..."
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<java.util.LinkedHashMap<String, String>> vnpayIPN(
            @RequestParam Map<String, String> params) {
        String rspCode = paymentService.handleVNPayIPN(params);
        String message = switch (rspCode) {
            case "00" -> "Confirm Success";
            case "97" -> "Invalid Checksum";
            case "01" -> "Order Not Found";
            case "02" -> "Order Already Confirmed";
            default -> "Unknown Error";
        };
        java.util.LinkedHashMap<String, String> body = new java.util.LinkedHashMap<>();
        body.put("RspCode", rspCode);
        body.put("Message", message);
        return ResponseEntity.ok(body);
    }

    // Xem lịch sử đơn hàng của tôi (yêu cầu đăng nhập)
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentOrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentOrderResponse> orders = paymentService.getMyOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Get my orders successfully", orders));
    }

    // Admin: xem tất cả đơn hàng
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentOrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentOrderResponse> orders = paymentService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Get all orders successfully", orders));
    }

    // Lấy IP thực của client
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
