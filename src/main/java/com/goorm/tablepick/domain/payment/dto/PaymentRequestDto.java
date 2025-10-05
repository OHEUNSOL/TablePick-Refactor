package com.goorm.tablepick.domain.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {
    private Long reservationId;
    private Long memberId;
    private Long amount;
}