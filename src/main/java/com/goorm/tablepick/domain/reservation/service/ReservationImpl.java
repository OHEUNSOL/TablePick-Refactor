package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.dto.response.CreateReservationResponseDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.enums.ReservationStatus;
import com.goorm.tablepick.domain.reservation.exception.ReservationErrorCode;
import com.goorm.tablepick.domain.reservation.exception.ReservationException;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantErrorCode;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantException;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public CreateReservationResponseDto createReservation(String username, ReservationRequestDto request) {
        // 식당 검증
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));
        
        // 멤버 검증
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        // 예약 가능 시간 확인
        ReservationSlot reservationSlot = reservationSlotRepository.findByRestaurantIdAndDateAndTime(
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
        
        Reservation savedReservation = reservationRepository.save(reservation);
        // 슬롯 카운트 증가
        reservationSlot.setCount(count + 1);
        reservationSlotRepository.save(reservationSlot);
        
        // 비동기 결제 요청
        // requestPaymentAsync(paymentId, request, member, restaurant);
        
        CreateReservationResponseDto dto = CreateReservationResponseDto.builder()
                .reservationId(savedReservation.getId())
                .build();
        
        return dto;
    }

    @Override
    @Transactional
    public void cancelReservation(String username, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NOT_FOUND));
        
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        if (!reservation.getMember().equals(member)) {
            throw new ReservationException(ReservationErrorCode.UNAUTHORIZED_CANCEL);
        }
        
        if (reservation.getReservationStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationException(ReservationErrorCode.ALREADY_CANCELLED);
        }
        
        // 예약 및 결제 상태 변경
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservation.setPaymentStatus("CANCELLED");
        reservationRepository.save(reservation);
        
        ReservationSlot reservationSlot = reservationSlotRepository.findById(reservation.getReservationSlot().getId())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NO_RESERVATION_SLOT));
        reservationSlot.setCount(Math.max(0, reservationSlot.getCount() - 1));
        reservationSlotRepository.save(reservationSlot);

    }
    
    public List<String> getAvailableReservationTimes(Long restaurantId, LocalDate date) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantException(RestaurantErrorCode.NOT_FOUND));
        
        List<ReservationSlot> reservationTimes = reservationSlotRepository.findAvailableTimes(restaurantId, date);
        
        return reservationTimes.stream()
                .map(slot -> slot.getTime().truncatedTo(ChronoUnit.MINUTES))
                .distinct()
                .sorted()
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm"))) // 문자열 변환
                .toList();
    }
    
    private Long calculateAmount(Long partySize, Restaurant restaurant) {
        return partySize * 5000L; // 인원당 5,000원
    }
    
}
