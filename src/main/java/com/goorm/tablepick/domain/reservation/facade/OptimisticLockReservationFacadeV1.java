package com.goorm.tablepick.domain.reservation.facade;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.service.ReservationNotificationService;
import com.goorm.tablepick.domain.reservation.service.ReservationService.ReservationServiceV2;
import com.goorm.tablepick.domain.reservation.service.ReservationService.ReservationServiceV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockReservationFacadeV1 {
    private final ReservationServiceV3 reservationServiceV3;
    private final ReservationNotificationService reservationNotificationService;

    private static final long RETRY_DELAY_MS = 30;
    private static final int MAX_RETRY_COUNT = 5; // 재시도 최대 횟수 추가

    public void createReservation(String userName, ReservationRequestDto request)
            throws InterruptedException {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // 1. 트랜잭션 안에서 예약 + 결제 처리
                Reservation reservation = reservationServiceV3.createReservationOptimistic(userName, request);
                log.info("예약 생성 성공 - username: {}, 총 시도횟수: {}",
                        userName, retryCount + 1);
                // 2. 트랜잭션 밖에서 알림 실행
                reservationNotificationService
                        .sendReservationCreatedNotification(reservation.getId());

                return; // 성공 시 메서드 종료

            } catch (Exception e) {
                retryCount++;
                log.warn("예약 생성 재시도 - username: {}, 현재 시도횟수: {}, error: {}",
                        userName, retryCount, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }
}
