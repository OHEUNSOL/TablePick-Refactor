package com.goorm.tablepick.domain.reservation.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PaymentRequestEvent {
    private Long reservationId;
    private Long memberId;
    private Long amount;
}
