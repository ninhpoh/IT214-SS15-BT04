package com.example.concert.controller;

import com.example.concert.model.ReservationRequest;
import com.example.concert.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho Concert Service.
 *
 * Endpoints:
 * - POST /api/concerts/reserve          : Giữ chỗ
 * - POST /api/concerts/simulate/failure : Bật/tắt mô phỏng lỗi
 */
@Slf4j
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final SeatReservationService seatReservationService;

    /**
     * API giữ chỗ cho concert.
     */
    @PostMapping("/reserve")
    public Map<String, Object> reserveSeats(@RequestBody ReservationRequest request) {
        return seatReservationService.reserveSeats(request);
    }

    /**
     * Bật/tắt mô phỏng reservation failure để test Compensation.
     * Ví dụ: POST /api/concerts/simulate/failure?enable=true
     */
    @PostMapping("/simulate/failure")
    public Map<String, Object> simulateFailure(@RequestParam boolean enable) {
        seatReservationService.setSimulateFailure(enable);
        log.info("[ConcertService] Simulate failure: {}", enable);
        return Map.of("message", "Reservation failure simulation: " + enable);
    }
}
