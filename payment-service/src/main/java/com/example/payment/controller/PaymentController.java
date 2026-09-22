package com.example.payment.controller;

import com.example.payment.model.PaymentRequest;
import com.example.payment.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho Payment Service.
 *
 * Endpoints:
 * - POST /api/payments/process         : Xử lý thanh toán
 * - POST /api/payments/refund          : Hoàn tiền (compensation)
 * - POST /api/payments/simulate/failure : Bật/tắt mô phỏng lỗi
 * - POST /api/payments/simulate/timeout : Bật/tắt mô phỏng timeout
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentProcessingService paymentProcessingService;

    /**
     * API xử lý thanh toán.
     */
    @PostMapping("/process")
    public Map<String, Object> processPayment(@RequestBody PaymentRequest request) {
        return paymentProcessingService.processPayment(request);
    }

    /**
     * API hoàn tiền (Compensation).
     */
    @PostMapping("/refund")
    public Map<String, Object> refundPayment(@RequestBody Map<String, Object> request) {
        String bookingId = (String) request.get("bookingId");
        double amount = ((Number) request.get("amount")).doubleValue();
        return paymentProcessingService.refundPayment(bookingId, amount);
    }

    /**
     * Bật/tắt mô phỏng payment failure để test Saga.
     * Ví dụ: POST /api/payments/simulate/failure?enable=true
     */
    @PostMapping("/simulate/failure")
    public Map<String, Object> simulateFailure(@RequestParam boolean enable) {
        paymentProcessingService.setSimulateFailure(enable);
        log.info("[PaymentService] Simulate failure: {}", enable);
        return Map.of("message", "Payment failure simulation: " + enable);
    }

    /**
     * Bật/tắt mô phỏng timeout để test Retry Policy.
     * Ví dụ: POST /api/payments/simulate/timeout?enable=true
     */
    @PostMapping("/simulate/timeout")
    public Map<String, Object> simulateTimeout(@RequestParam boolean enable) {
        paymentProcessingService.setSimulateTimeout(enable);
        log.info("[PaymentService] Simulate timeout: {}", enable);
        return Map.of("message", "Payment timeout simulation: " + enable);
    }
}
