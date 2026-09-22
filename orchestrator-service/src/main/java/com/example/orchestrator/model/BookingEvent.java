package com.example.orchestrator.model;

/**
 * Enum định nghĩa các sự kiện (events) kích hoạt chuyển trạng thái
 * trong State Machine.
 *
 * Mỗi event tương ứng với một hành động hoặc kết quả từ Activity Service.
 */
public enum BookingEvent {

    PROCESS_PAYMENT,

    PAYMENT_SUCCESS,

    PAYMENT_FAILED,

    RESERVE_SEATS,

    RESERVATION_SUCCESS,

    RESERVATION_FAILED
}
