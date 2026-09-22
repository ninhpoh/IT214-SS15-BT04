package com.example.orchestrator.service;

import com.example.orchestrator.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service điều phối giữ chỗ - gọi Concert Service qua REST.
 * Không có Retry Policy cho reservation (theo yêu cầu bài).
 */
@Slf4j
@Service
public class ReservationOrchestrationService {

    private final RestClient concertRestClient;

    public ReservationOrchestrationService(@Qualifier("concertRestClient") RestClient concertRestClient) {
        this.concertRestClient = concertRestClient;
    }

    /**
     * Gọi Concert Service để giữ chỗ.
     *
     * @param transaction thông tin booking
     * @return true nếu giữ chỗ thành công, false nếu thất bại
     */
    @SuppressWarnings("unchecked")
    public boolean reserveSeats(BookingTransaction transaction) {
        // Chuẩn bị request body
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("bookingId", transaction.getBookingId());
        requestBody.put("concertCode", transaction.getConcertCode());
        requestBody.put("ticketQuantity", transaction.getTicketQuantity());

        try {
            // Gọi POST /api/concerts/reserve
            Map<String, Object> response = concertRestClient.post()
                    .uri("/api/concerts/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return (boolean) response.get("success");
            }

        } catch (Exception e) {
            log.error("[Orchestrator] Error calling Concert Service: {}", e.getMessage());
        }

        return false;
    }
}
