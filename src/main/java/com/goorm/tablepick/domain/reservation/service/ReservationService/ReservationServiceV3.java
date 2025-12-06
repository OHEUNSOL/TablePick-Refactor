package com.goorm.tablepick.domain.reservation.service.ReservationService;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.exception.MemberErrorCode;
import com.goorm.tablepick.domain.member.exception.MemberException;
import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.enums.ReservationStatus;
import com.goorm.tablepick.domain.reservation.exception.ReservationErrorCode;
import com.goorm.tablepick.domain.reservation.exception.ReservationException;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import com.goorm.tablepick.domain.reservation.service.ReservationNotificationService;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantErrorCode;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantException;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceV3 {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReservationNotificationService reservationNotificationService;

    @Transactional
    public Reservation createReservationPessimistic(String username, ReservationRequestDto request) {
        // 식당 검증
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));

        // 멤버 검증
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        // 예약 슬롯을 배타락으로 가져옴
        ReservationSlot reservationSlot = reservationSlotRepository.findWithPessimisticLock(
                        request.getRestaurantId(), request.getReservationDate(), request.getReservationTime())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NO_RESERVATION_SLOT));

        // 슬롯 카운트 검증
        Long count = reservationSlot.getCount();
        int maxCapacity = restaurant.getMaxCapacity();
        if (count >= maxCapacity) {
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        // 슬롯 카운트 증가
        reservationSlot.setCount(count + 1);
        reservationSlotRepository.save(reservationSlot);

        // 예약 생성
        Reservation reservation = Reservation.builder()
                .member(member)
                .reservationSlot(reservationSlot)
                .partySize(request.getPartySize())
                .reservationStatus(ReservationStatus.CONFIRMED)
                .restaurant(restaurant)
                .createdAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        return reservation;

    }

    @Transactional
    public Reservation createReservationOptimistic(String username, ReservationRequestDto request) {
        // 식당 검증
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));

        // 멤버 검증
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        // 예약 슬롯을 버저닝과 함께 낙관적락으로 가져옴
        ReservationSlot reservationSlot = reservationSlotRepository.findWithOptimisticLock(
                        request.getRestaurantId(), request.getReservationDate(), request.getReservationTime())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NO_RESERVATION_SLOT));

        // 슬롯 카운트 검증
        Long count = reservationSlot.getCount();
        int maxCapacity = restaurant.getMaxCapacity();
        if (count >= maxCapacity) {
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        // 슬롯 카운트 증가
        reservationSlot.setCount(count + 1);
        reservationSlotRepository.saveAndFlush(reservationSlot);

        // 예약 생성
        Reservation reservation = Reservation.builder()
                .member(member)
                .reservationSlot(reservationSlot)
                .partySize(request.getPartySize())
                .reservationStatus(ReservationStatus.CONFIRMED)
                .restaurant(restaurant)
                .createdAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        return reservation;

    }
}