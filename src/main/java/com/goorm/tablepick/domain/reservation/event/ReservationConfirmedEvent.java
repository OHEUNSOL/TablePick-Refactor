package com.goorm.tablepick.domain.reservation.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationConfirmedEvent {
    private Long reservationId;
    private String email;
    private String restaurantName;
    private LocalDateTime confirmedAt;
    private int partySize;
}
