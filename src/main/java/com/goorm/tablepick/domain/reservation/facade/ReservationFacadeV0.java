package com.goorm.tablepick.domain.reservation.facade;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
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
    private static final int MAX_RETRY_COUNT = 5; // 재시도 최대 횟수 추가

    public void createReservation(String username, ReservationRequestDto request)
            throws InterruptedException {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                reservationServiceV2.createReservationOptimistic(username, request);
                log.info("예약 생성 성공 - username: {}, 총 시도횟수: {}",
                        username, retryCount + 1);
                return; // 성공 시 메서드 종료

            } catch (Exception e) {
                retryCount++;
                log.warn("예약 생성 재시도 - username: {}, 현재 시도횟수: {}, error: {}",
                        username, retryCount, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }
}
