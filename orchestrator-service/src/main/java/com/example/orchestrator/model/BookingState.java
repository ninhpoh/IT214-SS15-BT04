package com.example.orchestrator.model;

/**
 * Enum định nghĩa các trạng thái trong State Machine của booking.
 *
 * Flow thành công:
 * INITIATED -> PAYMENT_PENDING -> PAYMENT_COMPLETED -> SEAT_RESERVING -> BOOKING_CONFIRMED
 *
 * Flow thất bại:
 * PAYMENT_PENDING -> CANCELLED (payment failed)
 * SEAT_RESERVING -> CANCELLED (reservation failed, trigger refund)
 */
public enum BookingState {

    INITIATED,

    PAYMENT_PENDING,

    PAYMENT_COMPLETED,

    SEAT_RESERVING,

    BOOKING_CONFIRMED,

    CANCELLED
}
