package com.example.orchestrator.listener;

import com.example.orchestrator.model.BookingEvent;
import com.example.orchestrator.model.BookingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listener ghi log mỗi khi State Machine chuyển trạng thái.
 *
 * Format log:
 * [Orchestrator] State: {oldState} -> Event: {event} -> New State: {newState}
 */
@Slf4j
@Component
public class StateChangeListener {

    /**
     * Ghi log khi trạng thái thay đổi.
     *
     * @param oldState  trạng thái trước khi chuyển
     * @param event     sự kiện kích hoạt chuyển trạng thái
     * @param newState  trạng thái sau khi chuyển
     */
    public void onStateChange(BookingState oldState, BookingEvent event, BookingState newState) {
        log.info("[Orchestrator] State: {} -> Event: {} -> New State: {}",
                oldState, event, newState);
    }
}
