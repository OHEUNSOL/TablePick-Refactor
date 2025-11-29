package com.goorm.tablepick.domain.reservation.facade.V0;

import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.payment.PgClient;
import com.goorm.tablepick.domain.payment.dto.PaymentRequestDto;
import com.goorm.tablepick.domain.payment.dto.PaymentResponseDto;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateReservationTestFacadeV0 {
    private final ReservationExternalUpdateService reservationExternalUpdateService;
    private final ReservationSlotRepository reservationSlotRepository;
    private final MemberRepository memberRepository;

    public void createReservationOptimistic(Long memberId, ReservationRequestDto request) {

        // 1. 내부 트랜잭션으로 예약 생성
        Reservation reservation = reservationExternalUpdateService.createReservationWithOptimisticTransaction(
                memberId,
                request
        );


        // 3. 외부 API 응답으로 참가자 정보 업데이트
        reservationExternalUpdateService.updateReservationPayment(reservation.getId(), paymentResponse.getPaymentId());
    }

    public void createReservationPessimistic(Long memberId, ReservationRequestDto request) {
        // 1. 내부 트랜잭션으로 예약 생성
        Reservation reservation = reservationExternalUpdateService.createReservationWithPessimisticTransaction(
                memberId,
                request
        );

        // 3. 외부 API 응답으로 참가자 정보 업데이트
        reservationExternalUpdateService.updateReservationPayment(reservation.getId(), paymentResponse.getPaymentId());
    }
}