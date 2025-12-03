package com.goorm.tablepick.infra;

import com.goorm.tablepick.domain.reservation.entity.Reservation;

public interface EmailSender {

    void sendReservationEmail(Long reservationId);
}
