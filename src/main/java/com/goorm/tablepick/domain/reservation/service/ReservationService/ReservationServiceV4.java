package com.goorm.tablepick.domain.reservation.service.ReservationService;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceV4 {
    private static final String SERVICE_NAME = "ImprovedReservationService";

    private final StringRedisTemplate redisTemplate;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final MeterRegistry meterRegistry;

    private String buildFullKey(Long restaurantId, LocalDate date, LocalTime time) {
        // 레디스에 들어갈 "만석 플래그" 키 형태
        return "reservation:full:" + restaurantId + ":" + date + ":" + time;
    }

    @Transactional
    public Reservation createReservationPessimistic(String username, ReservationRequestDto request) {
        String fullKey = buildFullKey(request.getRestaurantId(), request.getReservationDate(), request.getReservationTime());

        // 이미 꽉 찬 슬롯이면 DB 안 가고 바로 컷
        String fullFlag = redisTemplate.opsForValue().get(fullKey);
        if ("1".equals(fullFlag)) {
            meterRegistry.counter("cache.hit", "service", SERVICE_NAME).increment();
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        meterRegistry.counter("db.select", "service", SERVICE_NAME).increment();

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

        // DB 기준으로도 이미 꽉 찼으면 → Redis 플래그 세팅 + 예외
        if (count >= maxCapacity) {
            redisTemplate.opsForValue()
                    .set(fullKey, "1", 1, TimeUnit.HOURS);

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
        String fullKey = buildFullKey(request.getRestaurantId(), request.getReservationDate(), request.getReservationTime());

        // 이미 꽉 찬 슬롯이면 DB 안 가고 바로 컷
        String fullFlag = redisTemplate.opsForValue().get(fullKey);
        if ("1".equals(fullFlag)) {
            meterRegistry.counter("cache.hit", "service", SERVICE_NAME).increment();
            throw new ReservationException(ReservationErrorCode.EXCEED_RESERVATION_LIMIT);
        }

        meterRegistry.counter("db.select", "service", SERVICE_NAME).increment();

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

        // DB 기준으로도 이미 꽉 찼으면 → Redis 플래그 세팅 + 예외
        if (count >= maxCapacity) {
            redisTemplate.opsForValue()
                    .set(fullKey, "1", 1, TimeUnit.HOURS);

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

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelReservation(String username, Long reservationId) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NOT_FOUND));

        // 2. 본인 예약인지 검증 (이메일 기준)
        if (!reservation.getMember().getEmail().equals(username)) {
            throw new ReservationException(ReservationErrorCode.NO_AUTHORITY);
        }

        // 3. 이미 취소된 예약이면 예외 처리
        if (reservation.getReservationStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationException(ReservationErrorCode.ALREADY_CANCELLED);
        }

        // 4. 예약 상태 변경
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // 5. 슬롯 카운트 감소
        ReservationSlot reservationSlot = reservation.getReservationSlot();
        Restaurant restaurant = reservation.getRestaurant();

        Long currentCount = reservationSlot.getCount();
        if (currentCount > 0) {
            reservationSlot.setCount(currentCount - 1);
            reservationSlotRepository.save(reservationSlot);
        }

        // 6. 만석 플래그 해제
        String fullKey = buildFullKey(
                restaurant.getId(),
                reservationSlot.getDate(),
                reservationSlot.getTime()
        );

        redisTemplate.delete(fullKey);

        log.info("예약 취소 완료. reservationId={}, username={}", reservationId, username);
    }
}