package com.example.concert.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận request giữ chỗ từ Orchestrator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    private String bookingId;

    private String concertCode;

    private int ticketQuantity;
}
