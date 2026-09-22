package com.example.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận request thanh toán từ Orchestrator.
 * Cũng được sử dụng cho request refund (chỉ dùng bookingId và amount).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String bookingId;

    private String customerId;

    private double amount;
}
