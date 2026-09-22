package com.example.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận request từ client để tạo booking mới.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private String bookingId;

    private String concertCode;

    private String customerId;

    private String customerEmail;

    private int ticketQuantity;

    private double amount;
}
