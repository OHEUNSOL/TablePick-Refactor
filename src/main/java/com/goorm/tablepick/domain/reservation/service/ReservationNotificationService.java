package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.infra.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {

    private final EmailSender emailSender;

    public void sendReservationCreatedNotification(Long reservationId) {
        doSendMail(reservationId);
    }

    //비동기로 메일 전송
    @Async("mailTaskExecutor")
    public void sendReservationCreatedNotificationAsync(Long reservationId) {
        doSendMail(reservationId);
    }

    private void doSendMail(Long reservationId) {
        try {
            emailSender.sendReservationEmail(reservationId);
            log.info("예약 완료 메일 발송. reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("예약 완료 메일 발송 중 예외 발생. reservationId={}", reservationId, e);
        }
    }
}