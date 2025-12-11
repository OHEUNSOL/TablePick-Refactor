package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.infra.EmailSender;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {

    private final EmailSender emailSender;
    private final MeterRegistry meterRegistry;

    public void sendReservationCreatedNotification(Long reservationId) {
        try {
            emailSender.sendReservationEmail(reservationId);

            meterRegistry.counter("mail.sent.success", "type", "sync").increment();

            log.info("예약 완료 메일 발송. reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("예약 완료 메일 발송 중 예외 발생. reservationId={}", reservationId, e);
        }
    }

    //비동기로 메일 전송
    @Async("mailTaskExecutor")
    public void sendReservationCreatedNotificationAsync(Long reservationId) {
        try {
            emailSender.sendReservationEmail(reservationId);

            meterRegistry.counter("mail.sent.success", "type", "async").increment();

            log.info("예약 완료 메일 발송. reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("예약 완료 메일 발송 중 예외 발생. reservationId={}", reservationId, e);
        }
    }

}