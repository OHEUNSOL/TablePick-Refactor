package com.goorm.tablepick.domain.reservation.controller;

import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.event.ReservationConfirmedEvent;
import com.goorm.tablepick.domain.reservation.exception.ReservationErrorCode;
import com.goorm.tablepick.domain.reservation.exception.ReservationException;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import com.goorm.tablepick.domain.reservation.service.ReservationNotificationService;
import com.goorm.tablepick.global.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/email")
public class EmailTestController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReservationNotificationService reservationNotificationService;
    private final ReservationRepository reservationRepository;
    private final JsonUtils jsonUtils;

    /**
     * 동기 메일 발송 테스트
     * POST /api/test/notifications/reservations/{reservationId}/sync
     */
    @PostMapping("/reservations/{reservationId}/sync")
    public ResponseEntity<Void> sendSync(@PathVariable Long reservationId) {

        reservationNotificationService.sendReservationCreatedNotification(reservationId);

        return ResponseEntity.ok().build();
    }

    /**
     * 비동기(@Async) 메일 발송 테스트
     * POST /api/test/notifications/reservations/{reservationId}/async
     */
    @PostMapping("/reservations/{reservationId}/async")
    public ResponseEntity<Void> sendAsync(@PathVariable Long reservationId) {

        reservationNotificationService.sendReservationCreatedNotificationAsync(reservationId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservations/{reservationId}/kafka")
    public ResponseEntity<Void> sendKafka(@PathVariable Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NOT_FOUND));

        ReservationConfirmedEvent reservationConfirmedEvent = ReservationConfirmedEvent.builder()
                .reservationId(reservation.getId())
                .email(reservation.getMember().getEmail())
                .restaurantName(reservation.getRestaurantName())
                .confirmedAt(reservation.getCreatedAt())
                .partySize(reservation.getPartySize())
                .build();

        kafkaTemplate.send("reservation.confirmed", jsonUtils.toJsonString(reservationConfirmedEvent));

        return ResponseEntity.ok().build();
    }
}