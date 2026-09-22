package com.example.concert.service;

import com.example.concert.model.ReservationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ giữ chỗ và gán ghế.
 * KHÔNG chứa logic điều phối Saga - chỉ xử lý reservation.
 *
 * Hỗ trợ mô phỏng:
 * - Mặc định: giữ chỗ thành công
 * - simulateFailure = true: giữ chỗ thất bại (không đủ ghế)
 */
@Slf4j
@Service
public class SeatReservationService {

    // Flag để mô phỏng giữ chỗ thất bại
    private boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    /**
     * Xử lý giữ chỗ cho booking.
     */
    public Map<String, Object> reserveSeats(ReservationRequest request) {
        log.info("[ConcertService] Reserving {} seats for booking: {}",
                request.getTicketQuantity(), request.getBookingId());

        // Mô phỏng giữ chỗ thất bại
        if (simulateFailure) {
            log.info("[ConcertService] Reservation FAILED for booking: {} - Not enough seats",
                    request.getBookingId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "Reservation failed - not enough seats available");
            return response;
        }

        // Mặc định: giữ chỗ thành công
        log.info("[ConcertService] Seats reserved successfully");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Seats reserved successfully");
        return response;
    }
}
