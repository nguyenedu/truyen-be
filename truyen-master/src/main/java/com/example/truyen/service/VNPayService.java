package com.example.truyen.service;

import com.example.truyen.entity.PaymentOrder;

import java.util.Map;

public interface VNPayService {

    String createPaymentUrl(PaymentOrder order, String ipAddress);

    boolean verifyCallback(Map<String, String> params);
}
