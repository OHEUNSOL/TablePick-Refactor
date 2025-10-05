package com.goorm.tablepick.domain.reservation.service.ImprovedReservationService;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.exception.MemberErrorCode;
import com.goorm.tablepick.domain.member.exception.MemberException;
import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.payment.PgClient;
import com.goorm.tablepick.domain.payment.RestPaymentApi;
import com.goorm.tablepick.domain.payment.dto.PaymentRequestDto;
import com.goorm.tablepick.domain.payment.dto.PaymentResponseDto;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.enums.ReservationStatus;
import com.goorm.tablepick.domain.reservation.exception.ReservationErrorCode;
import com.goorm.tablepick.domain.reservation.exception.ReservationException;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import com.goorm.tablepick.domain.reservation.service.ReservationExternalUpdateService;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantErrorCode;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantException;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceV2 {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReservationExternalUpdateService reservationExternalUpdateService;
    private final RestPaymentApi paymentApi;
    private final PgClient pgClient;


    @Transactional
    public String createReservationPessimistic(String username, ReservationRequestDto request) {
        // 식당 검증
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));

        // 멤버 검증
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 예약 슬롯을 배타락으로 가져옴
        ReservationSlot reservationSlot = reservationSlotRepository.findWithPessimisticLock(
                        request.getRestaurantId(), request.getReservationDate(), request.getReservationTime())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NO_RESERVATION_SLOT));

        // 중복 예약 확인
        boolean hasDuplicate = reservationRepository.findByReservationSlot(reservationSlot).stream()
                .anyMatch(r -> r.getMember().equals(member) && r.getReservationStatus() == ReservationStatus.CONFIRMED);
        if (hasDuplicate) {
            throw new ReservationException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }

        // 슬롯 카운트 검증
        Long count = reservationSlot.getCount();
        Long maxCapacity = restaurant.getMaxCapacity();
        if (count >= maxCapacity) {
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        // 슬롯 카운트 증가
        reservationSlot.setCount(count + 1);
        reservationSlotRepository.save(reservationSlot);

        // 예약 생성 (PENDING)
        String paymentId = UUID.randomUUID().toString();
        Reservation reservation = Reservation.builder()
                .member(member)
                .reservationSlot(reservationSlot)
                .partySize(request.getPartySize())
                .reservationStatus(ReservationStatus.CONFIRMED)
                .restaurant(restaurant)
                .paymentId(paymentId)
                .paymentStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        return paymentId;
    }

    @Transactional
    public void createReservationOptimistic(Long memberId, ReservationRequestDto request) {
        // 식당 검증
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));

        // 0. 멤버 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        // 예약 슬롯을 버저닝과 함께 낙관적락으로 가져옴
        ReservationSlot reservationSlot = reservationSlotRepository.findWithOptimisticLock(
                        request.getRestaurantId(), request.getReservationDate(), request.getReservationTime())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NO_RESERVATION_SLOT));

        // 중복 예약 확인
        boolean hasDuplicate = reservationRepository.findByReservationSlot(reservationSlot).stream()
                .anyMatch(r -> r.getMember().equals(member) && r.getReservationStatus() != ReservationStatus.CANCELLED);
        if (hasDuplicate) {
            throw new ReservationException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }

        // 슬롯 카운트 검증
        Long count = reservationSlot.getCount();
        Long maxCapacity = restaurant.getMaxCapacity();
        if (count >= maxCapacity) {
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        // 슬롯 카운트 증가
        reservationSlot.setCount(count + 1);
        reservationSlotRepository.saveAndFlush(reservationSlot);

        // 예약 생성 (PENDING)

        Reservation reservation = Reservation.builder()
                .member(member)
                .reservationSlot(reservationSlot)
                .partySize(request.getPartySize())
                .reservationStatus(ReservationStatus.PENDING)
                .restaurant(restaurant)
                .paymentStatus("PENDING")
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 2. 외부 결제 API 호출
        PaymentResponseDto paymentResponse = pgClient.callPgApi( // PgClient를 통해 Fake PG 서버 호출
                PaymentRequestDto.builder()
                        .reservationId(reservation.getId())
                        .memberId(member.getId())
                        .amount(request.getPartySize() * 5000L)
                        .build()
        );
        if (!paymentResponse.isSuccess()) {
            log.error("외부 결제 API 호출 실패. 예약 ID: {}, 오류: {}", reservation.getId(), paymentResponse.getErrorMessage());
        }

        // 3. 외부 API 응답으로 참가자 정보 업데이트
        reservationExternalUpdateService.updateReservationPayment(reservation.getId(), paymentResponse.getPaymentId());


    }
}