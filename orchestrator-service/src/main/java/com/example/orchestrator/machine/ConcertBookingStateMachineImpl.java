package com.example.orchestrator.machine;

import com.example.orchestrator.listener.StateChangeListener;
import com.example.orchestrator.model.*;
import com.example.orchestrator.service.PaymentOrchestrationService;
import com.example.orchestrator.service.ReservationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation của State Machine cho Concert Booking Saga.
 *
 * ĐÂY LÀ "NHẠC TRƯỞNG" (Orchestrator):
 * - Quản lý bảng chuyển trạng thái (transition table)
 * - Điều phối các Activity Service (Payment, Concert)
 * - Kích hoạt Compensation khi giao dịch thất bại
 *
 * KHÔNG chứa business logic:
 * - Không kiểm tra balance, inventory, seat availability
 * - Các logic đó nằm trong PaymentService và ConcertService
 *
 * State Transitions:
 * ┌─────────────┐  PROCESS_PAYMENT  ┌─────────────────┐
 * │  INITIATED  │ ───────────────── │ PAYMENT_PENDING  │
 * └─────────────┘                   └─────────────────┘
 *                                     │             │
 *                          PAYMENT_SUCCESS    PAYMENT_FAILED
 *                                     │             │
 *                              ┌──────────────┐  ┌──────────┐
 *                              │ PAYMENT_     │  │ CANCELLED│
 *                              │ COMPLETED    │  └──────────┘
 *                              └──────────────┘
 *                                     │
 *                               RESERVE_SEATS
 *                                     │
 *                              ┌──────────────┐
 *                              │ SEAT_        │
 *                              │ RESERVING    │
 *                              └──────────────┘
 *                                │           │
 *                    RESERVATION_SUCCESS  RESERVATION_FAILED
 *                                │           │
 *                         ┌──────────────┐ ┌──────────┐
 *                         │ BOOKING_     │ │ CANCELLED│
 *                         │ CONFIRMED   │ │ + REFUND │
 *                         └──────────────┘ └──────────┘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConcertBookingStateMachineImpl implements ConcertBookingStateMachine {

    private final PaymentOrchestrationService paymentOrchestrationService;
    private final ReservationOrchestrationService reservationOrchestrationService;
    private final StateChangeListener stateChangeListener;

    /**
     * In-memory storage: lưu trạng thái giao dịch theo bookingId.
     */
    private final Map<String, BookingTransaction> transactionStore = new ConcurrentHashMap<>();

    /**
     * Bảng chuyển trạng thái (Transition Table).
     * Key: trạng thái hiện tại
     * Value: Map<Event, trạng thái mới>
     */
    private static final Map<BookingState, Map<BookingEvent, BookingState>> TRANSITIONS = Map.of(
            BookingState.INITIATED, Map.of(
                    BookingEvent.PROCESS_PAYMENT, BookingState.PAYMENT_PENDING
            ),
            BookingState.PAYMENT_PENDING, Map.of(
                    BookingEvent.PAYMENT_SUCCESS, BookingState.PAYMENT_COMPLETED,
                    BookingEvent.PAYMENT_FAILED, BookingState.CANCELLED
            ),
            BookingState.PAYMENT_COMPLETED, Map.of(
                    BookingEvent.RESERVE_SEATS, BookingState.SEAT_RESERVING
            ),
            BookingState.SEAT_RESERVING, Map.of(
                    BookingEvent.RESERVATION_SUCCESS, BookingState.BOOKING_CONFIRMED,
                    BookingEvent.RESERVATION_FAILED, BookingState.CANCELLED
            )
    );

    @Override
    public BookingTransaction processBooking(BookingRequest request) {
        // Bước 0: Tạo transaction với trạng thái INITIATED
        BookingTransaction transaction = BookingTransaction.fromRequest(request);
        transactionStore.put(request.getBookingId(), transaction);

        // ===== PHASE 1: PAYMENT =====

        // Bước 1: INITIATED -> PAYMENT_PENDING
        fireEvent(transaction, BookingEvent.PROCESS_PAYMENT);

        // Bước 2: Gọi Payment Service (có Retry Policy)
        boolean paymentSuccess = paymentOrchestrationService.processPayment(transaction);

        if (paymentSuccess) {
            // Bước 3: PAYMENT_PENDING -> PAYMENT_COMPLETED
            fireEvent(transaction, BookingEvent.PAYMENT_SUCCESS);

            // ===== PHASE 2: RESERVATION =====

            // Bước 4: PAYMENT_COMPLETED -> SEAT_RESERVING
            fireEvent(transaction, BookingEvent.RESERVE_SEATS);

            // Bước 5: Gọi Concert Service
            boolean reservationSuccess = reservationOrchestrationService.reserveSeats(transaction);

            if (reservationSuccess) {
                // Bước 6: SEAT_RESERVING -> BOOKING_CONFIRMED
                fireEvent(transaction, BookingEvent.RESERVATION_SUCCESS);
                transaction.setMessage("Booking confirmed successfully!");
            } else {
                // Bước 6 (thất bại): SEAT_RESERVING -> CANCELLED
                fireEvent(transaction, BookingEvent.RESERVATION_FAILED);
                transaction.setMessage("Booking cancelled - reservation failed. Payment refunded.");

                // ===== COMPENSATION: REFUND PAYMENT =====
                // Payment đã thành công -> cần hoàn tiền
                paymentOrchestrationService.refundPayment(transaction);
            }

        } else {
            // Bước 3 (thất bại): PAYMENT_PENDING -> CANCELLED
            fireEvent(transaction, BookingEvent.PAYMENT_FAILED);
            transaction.setMessage("Booking cancelled - payment failed.");
            // Payment chưa thành công -> KHÔNG refund
        }

        // Log trạng thái cuối cùng
        log.info("[Orchestrator] Final State: {} for booking {}",
                transaction.getCurrentState(), transaction.getBookingId());

        return transaction;
    }

    @Override
    public BookingTransaction getTransaction(String bookingId) {
        return transactionStore.get(bookingId);
    }

    /**
     * Chuyển trạng thái dựa trên Transition Table và ghi log.
     *
     * @param transaction giao dịch cần chuyển trạng thái
     * @param event       sự kiện kích hoạt
     */
    private void fireEvent(BookingTransaction transaction, BookingEvent event) {
        BookingState currentState = transaction.getCurrentState();

        // Tìm trạng thái mới từ Transition Table
        Map<BookingEvent, BookingState> possibleTransitions = TRANSITIONS.get(currentState);

        if (possibleTransitions == null || !possibleTransitions.containsKey(event)) {
            log.error("[Orchestrator] Invalid transition: State={}, Event={}", currentState, event);
            throw new IllegalStateException(
                    "No transition defined for State=" + currentState + ", Event=" + event);
        }

        BookingState newState = possibleTransitions.get(event);

        // Cập nhật trạng thái
        transaction.setCurrentState(newState);

        // Ghi log chuyển trạng thái
        stateChangeListener.onStateChange(currentState, event, newState);
    }
}
