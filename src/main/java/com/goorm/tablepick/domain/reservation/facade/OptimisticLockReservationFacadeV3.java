package com.goorm.tablepick.domain.reservation.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.event.ReservationConfirmedEvent;
import com.goorm.tablepick.domain.reservation.service.ReservationNotificationService;
import com.goorm.tablepick.domain.reservation.service.ReservationService.ReservationServiceV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockReservationFacadeV3 {
    private final ReservationServiceV3 reservationServiceV3;
    private final ReservationNotificationService reservationNotificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final long RETRY_DELAY_MS = 30;
    private static final int MAX_RETRY_COUNT = 5; // 재시도 최대 횟수 추가

    public void createReservation(String memberName, ReservationRequestDto request)
            throws InterruptedException {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // 1. 트랜잭션 안에서 예약 + 결제 처리
                Reservation reservation = reservationServiceV3.createReservationOptimistic(memberName, request);
                log.info("예약 생성 성공 - username: {}, 총 시도횟수: {}",
                        memberName, retryCount + 1);

                ReservationConfirmedEvent reservationConfirmedEvent = ReservationConfirmedEvent.builder()
                        .reservationId(reservation.getId())
                        .email(memberName)
                        .restaurantName(reservation.getRestaurantName())
                        .confirmedAt(reservation.getCreatedAt())
                        .partySize(reservation.getPartySize())
                        .build();

                kafkaTemplate.send("reservation.confirmed", toJsonString(reservationConfirmedEvent));

                return; // 성공 시 메서드 종료

            } catch (Exception e) {
                retryCount++;
                log.warn("예약 생성 재시도 - username: {}, 현재 시도횟수: {}, error: {}",
                        memberName, retryCount, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }

    private String toJsonString(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String message = objectMapper.writeValueAsString(object);
            return message;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json 직렬화 실패");
        }
    }
}
