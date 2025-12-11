package com.goorm.tablepick.domain.reservation.facade;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.service.ReservationNotificationService;
import com.goorm.tablepick.domain.reservation.service.ReservationService.ReservationServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationFacadeV0 {
    private final ReservationServiceV2 reservationServiceV2;

    private static final long RETRY_DELAY_MS = 30;
    private static final int MAX_RETRY_COUNT = 10; // 재시도 최대 횟수 추가

    public void createReservation(String username, ReservationRequestDto request)
            throws InterruptedException {

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                reservationServiceV2.createReservationOptimistic(username, request);
                log.info("예약 생성 성공 - username: {}, 총 시도횟수: {}",
                        username, retryCount + 1);
                return; // ✅ 성공하면 바로 종료

            } catch (Exception e) {
                retryCount++;
                lastException = e;

                log.warn("예약 생성 재시도 - username: {}, 현재 시도횟수: {}, error: {}",
                        username, retryCount, e.getClass().getSimpleName(), e);

                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        // ✅ 여기서 원인 예외를 같이 던져야 무슨 예외인지 알 수 있음
        throw new RuntimeException(
                "접속량이 많아 예약에 실패했습니다. 잠시 후 다시 시도해주세요.",
                lastException
        );
    }
}
