package com.goorm.tablepick.domain.reservation.facade.V0;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.exception.MemberErrorCode;
import com.goorm.tablepick.domain.member.exception.MemberException;
import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateReservationFacadeV0 {
    private final ReservationExternalUpdateService reservationExternalUpdateService;
    private final ReservationSlotRepository reservationSlotRepository;
    private final MemberRepository memberRepository;

    public void createReservation(String email, ReservationRequestDto request) {
        // 0. 멤버 검증
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        // 1. 내부 트랜잭션으로 예약 생성
        Reservation reservation = reservationExternalUpdateService.createReservationWithOptimisticTransaction(
                member.getId(),
                request
        );

    }
}