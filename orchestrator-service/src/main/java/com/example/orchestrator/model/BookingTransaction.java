package com.example.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đối tượng lưu trữ thông tin giao dịch booking và trạng thái hiện tại.
 * Được lưu trong ConcurrentHashMap (in-memory) để theo dõi trạng thái.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTransaction {

    private String bookingId;

    private String concertCode;

    private String customerId;

    private String customerEmail;

    private int ticketQuantity;

    private double amount;

    private BookingState currentState;

    private String message;

    /**
     * Tạo BookingTransaction từ BookingRequest với trạng thái ban đầu INITIATED.
     */
    public static BookingTransaction fromRequest(BookingRequest request) {
        return BookingTransaction.builder()
                .bookingId(request.getBookingId())
                .concertCode(request.getConcertCode())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .ticketQuantity(request.getTicketQuantity())
                .amount(request.getAmount())
                .currentState(BookingState.INITIATED)
                .build();
    }
}
