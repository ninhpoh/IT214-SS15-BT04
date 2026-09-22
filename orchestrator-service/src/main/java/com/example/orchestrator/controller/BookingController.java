package com.example.orchestrator.controller;

import com.example.orchestrator.machine.ConcertBookingStateMachine;
import com.example.orchestrator.model.BookingRequest;
import com.example.orchestrator.model.BookingTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho Orchestrator Service.
 *
 * Endpoints:
 * - POST /api/bookings          : Tạo booking mới và chạy Saga
 * - GET  /api/bookings/{id}     : Kiểm tra trạng thái booking
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final ConcertBookingStateMachine stateMachine;

    /**
     * Tạo booking mới.
     * Orchestrator sẽ điều phối toàn bộ luồng Saga:
     * Payment -> Reservation -> Confirmed (hoặc Cancelled + Compensation)
     */
    @PostMapping
    public ResponseEntity<BookingTransaction> createBooking(@RequestBody BookingRequest request) {
        log.info("[Orchestrator] Received booking request: {}", request.getBookingId());

        BookingTransaction result = stateMachine.processBooking(request);

        return ResponseEntity.ok(result);
    }

    /**
     * Kiểm tra trạng thái của booking.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingTransaction> getBooking(@PathVariable String bookingId) {
        BookingTransaction transaction = stateMachine.getTransaction(bookingId);

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transaction);
    }
}
