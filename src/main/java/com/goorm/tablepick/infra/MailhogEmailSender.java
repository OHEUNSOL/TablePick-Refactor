package com.goorm.tablepick.infra;

import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.exception.ReservationErrorCode;
import com.goorm.tablepick.domain.reservation.exception.ReservationException;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailhogEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public void sendReservationEmail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.NOT_FOUND));

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(reservation.getMember().getEmail());
            helper.setFrom("no-reply@tablepick.com");
            helper.setSubject("[TablePick] 예약이 완료되었습니다.");

            String dateText = reservation.getCreatedAt()
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            String timeText = reservation.getCreatedAt()
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String text = String.format(
                    "고객님의 %s 예약이 완료되었습니다.\n\n" +
                            "- 매장: %s\n" +
                            "- 예약 확정 시각: %s %s\n" +
                            "- 인원: %d명\n" +
                            "- 예약 번호: %d\n",
                    reservation.getRestaurant().getName(),
                    reservation.getRestaurant().getName(),
                    dateText,
                    timeText,
                    reservation.getPartySize(),
                    reservation.getId()
            );

            helper.setText(text, false);

            mailSender.send(message);
            log.info("예약 완료 메일 발송 성공. reservationId={}, to={}",
                    reservation.getId(), reservation.getMember().getEmail());

        } catch (MessagingException e) {
            log.error("예약 완료 메일 발송 실패. reservationId={}", reservation.getId(), e);
        }
    }
}