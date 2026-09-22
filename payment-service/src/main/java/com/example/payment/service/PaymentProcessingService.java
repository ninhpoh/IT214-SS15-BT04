package com.example.payment.service;

import com.example.payment.model.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ thanh toán.
 * KHÔNG chứa logic điều phối Saga - chỉ xử lý payment.
 *
 * Hỗ trợ mô phỏng các trường hợp:
 * - Mặc định: thanh toán thành công
 * - simulateFailure = true: thanh toán thất bại (insufficient funds)
 * - simulateTimeout = true: thanh toán bị timeout (sleep lâu)
 */
@Slf4j
@Service
public class PaymentProcessingService {

    // Flag để mô phỏng thanh toán thất bại
    private boolean simulateFailure = false;

    // Flag để mô phỏng timeout
    private boolean simulateTimeout = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    /**
     * Xử lý thanh toán cho booking.
     */
    public Map<String, Object> processPayment(PaymentRequest request) {
        log.info("[PaymentService] Processing payment for booking: {}", request.getBookingId());

        // Mô phỏng timeout - sleep 10 giây để RestClient timeout (3 giây)
        if (simulateTimeout) {
            log.info("[PaymentService] Simulating timeout for booking: {}", request.getBookingId());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Mô phỏng thanh toán thất bại
        if (simulateFailure) {
            log.info("[PaymentService] Payment FAILED for booking: {}", request.getBookingId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "Payment failed - insufficient funds");
            return response;
        }

        // Mặc định: thanh toán thành công
        log.info("[PaymentService] Payment completed successfully");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Payment completed");
        return response;
    }

    /**
     * Xử lý hoàn tiền (refund) khi Saga compensation được kích hoạt.
     */
    public Map<String, Object> refundPayment(String bookingId, double amount) {
        log.info("[PaymentService] Processing refund for booking: {}, amount: {}", bookingId, amount);
        log.info("[PaymentService] Refund successfully for booking: {}", bookingId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Refund completed for booking: " + bookingId);
        return response;
    }
}
