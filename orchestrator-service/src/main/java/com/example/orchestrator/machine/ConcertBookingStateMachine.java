package com.example.orchestrator.machine;

import com.example.orchestrator.model.BookingRequest;
import com.example.orchestrator.model.BookingTransaction;

/**
 * Interface cho State Machine quản lý luồng booking.
 *
 * State Machine chỉ:
 * 1. Nhận trạng thái hiện tại
 * 2. Nhận Event
 * 3. Chuyển trạng thái
 * 4. Gọi Service tương ứng
 * 5. Nhận kết quả
 * 6. Chuyển trạng thái tiếp theo
 * 7. Trigger Compensation nếu thất bại
 *
 * KHÔNG chứa business logic (if balance < amount, if seatAvailable, ...).
 */
public interface ConcertBookingStateMachine {

    /**
     * Xử lý toàn bộ luồng booking từ INITIATED đến kết quả cuối cùng.
     *
     * @param request thông tin booking từ client
     * @return BookingTransaction chứa trạng thái cuối cùng
     */
    BookingTransaction processBooking(BookingRequest request);

    /**
     * Lấy thông tin giao dịch theo bookingId.
     *
     * @param bookingId mã booking
     * @return BookingTransaction hoặc null nếu không tìm thấy
     */
    BookingTransaction getTransaction(String bookingId);
}
