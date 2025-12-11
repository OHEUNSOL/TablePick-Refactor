package com.goorm.tablepick.domain.reservation.facade;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.event.ReservationConfirmedEvent;
import com.goorm.tablepick.domain.reservation.service.ReservationService.ReservationServiceV4;
import com.goorm.tablepick.global.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationFacadeV4 {
    private final ReservationServiceV4 reservationServiceV4;
    private final JsonUtils  jsonUtils;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final long RETRY_DELAY_MS = 30;
    private static final int MAX_RETRY_COUNT = 5; // 재시도 최대 횟수 추가

    public void createReservation(String memberName, ReservationRequestDto request)
            throws InterruptedException {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // 1. 트랜잭션 안에서 예약 + 결제 처리
                Reservation reservation = reservationServiceV4.createReservationOptimistic(memberName, request);
                log.info("예약 생성 성공 - username: {}, 총 시도횟수: {}",
                        memberName, retryCount + 1);

                ReservationConfirmedEvent reservationConfirmedEvent = ReservationConfirmedEvent.builder()
                        .reservationId(reservation.getId())
                        .email(memberName)
                        .restaurantName(reservation.getRestaurantName())
                        .confirmedAt(reservation.getCreatedAt())
                        .partySize(reservation.getPartySize())
                        .build();

                kafkaTemplate.send("reservation.confirmed", jsonUtils.toJsonString(reservationConfirmedEvent));

                return; // 성공 시 메서드 종료

            } catch (Exception e) {
                retryCount++;
                log.warn("예약 생성 재시도 - username: {}, 현재 시도횟수: {}, error: {}",
                        memberName, retryCount, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }

    public void createReservationPessimistic(String memberName, ReservationRequestDto request) {
        Reservation reservation = reservationServiceV4.createReservationPessimistic(memberName, request);

        ReservationConfirmedEvent reservationConfirmedEvent = ReservationConfirmedEvent.builder()
                .reservationId(reservation.getId())
                .email(memberName)
                .restaurantName(reservation.getRestaurantName())
                .confirmedAt(reservation.getCreatedAt())
                .partySize(reservation.getPartySize())
                .build();

        kafkaTemplate.send("reservation.confirmed", jsonUtils.toJsonString(reservationConfirmedEvent));

        return;
    }

}
