package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service điều phối thanh toán - gọi Payment Service qua REST.
 *
 * Chứa Retry Policy:
 * - Maximum attempts: 3
 * - Delay: 2000ms giữa các lần retry
 * - Chỉ retry khi gặp network/timeout exception (ResourceAccessException)
 * - KHÔNG retry khi payment trả về success=false (business failure)
 *
 * Cũng xử lý Compensation (refund) khi cần.
 */
@Slf4j
@Service
public class PaymentOrchestrationService {

    private final RestClient paymentRestClient;

    private static final int MAX_ATTEMPTS = 3;
    private static final int DELAY_MS = 2000;

    public PaymentOrchestrationService(@Qualifier("paymentRestClient") RestClient paymentRestClient) {
        this.paymentRestClient = paymentRestClient;
    }

    /**
     * Gọi Payment Service để xử lý thanh toán, có Retry Policy.
     *
     * @param transaction thông tin booking
     * @return true nếu thanh toán thành công, false nếu thất bại
     */
    @SuppressWarnings("unchecked")
    public boolean processPayment(BookingTransaction transaction) {
        // Chuẩn bị request body
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("bookingId", transaction.getBookingId());
        requestBody.put("customerId", transaction.getCustomerId());
        requestBody.put("amount", transaction.getAmount());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            log.info("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt {}/{}",
                    attempt, MAX_ATTEMPTS);

            try {
                // Gọi POST /api/payments/process
                Map<String, Object> response = paymentRestClient.post()
                        .uri("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                if (response != null) {
                    boolean success = (boolean) response.get("success");
                    // Nếu payment trả về kết quả (dù success hay fail) -> KHÔNG retry
                    // Chỉ retry khi gặp exception (network/timeout)
                    return success;
                }

            } catch (ResourceAccessException e) {
                // Network error hoặc Timeout -> retry
                log.warn("[Orchestrator] RetryPolicy: Attempt {}/{} failed - {}",
                        attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    log.info("[Orchestrator] RetryPolicy: Waiting {}ms before next attempt...", DELAY_MS);
                    try {
                        Thread.sleep(DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            } catch (Exception e) {
                // Lỗi không mong đợi -> không retry
                log.error("[Orchestrator] Unexpected error during payment: {}", e.getMessage());
                return false;
            }
        }

        // Sau MAX_ATTEMPTS lần vẫn thất bại
        log.error("[Orchestrator] RetryPolicy: All {} attempts failed for booking: {}",
                MAX_ATTEMPTS, transaction.getBookingId());
        return false;
    }

    /**
     * Gọi Payment Service để hoàn tiền (Compensation).
     * Chỉ gọi khi payment đã thành công trước đó.
     *
     * @param transaction thông tin booking cần hoàn tiền
     */
    @SuppressWarnings("unchecked")
    public void refundPayment(BookingTransaction transaction) {
        log.info("[Orchestrator] Compensation triggered for booking: {}", transaction.getBookingId());

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("bookingId", transaction.getBookingId());
        requestBody.put("amount", transaction.getAmount());

        try {
            paymentRestClient.post()
                    .uri("/api/payments/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("[Orchestrator] Refund failed for booking: {} - {}",
                    transaction.getBookingId(), e.getMessage());
        }
    }
}
