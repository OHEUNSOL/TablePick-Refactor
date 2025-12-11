package com.goorm.tablepick.domain.reservation.controller;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.facade.*;
import com.goorm.tablepick.domain.reservation.service.ReservationService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 동시성/외부 시스템 연동/Redis 전략을
 * 버전별로 분리해서 테스트하기 위한 전용 컨트롤러.
 *
 * URL 패턴 요약
 *  - V0: /api/reservations/test/v0
 *  - V1: /api/reservations/test/v1
 *  - V2: /api/reservations/test/v2
 *  - V3 Sync:  /api/reservations/test/v3/sync
 *  - V3 Async: /api/reservations/test/v3/async
 *  - V3 Kafka: /api/reservations/test/v3/kafka
 *  - V4 Kafka+Redis: POST /api/reservations/test/v4/kafka-redis
 *  - V4 취소:        DELETE /api/reservations/test/v4/{reservationId}
 */
@RestController
@RequestMapping("/api/reservations/test")
@RequiredArgsConstructor
public class ReservationTestController {

    // ===== Service 계층 (DB + 락 전략) =====
    private final ReservationServiceV0 reservationServiceV0;
    private final ReservationServiceV1 reservationServiceV1;
    private final ReservationServiceV2 reservationServiceV2;
    private final ReservationServiceV3 reservationServiceV3;
    private final ReservationServiceV4 reservationServiceV4;

    // ===== Facade 계층 (외부 기능 실행 방식 차이) =====
    private final ReservationFacadeV0 reservationFacadeV0;
    private final ReservationFacadeV1 reservationFacadeV1;
    private final ReservationFacadeV2 reservationFacadeV2;
    private final ReservationFacadeV3 reservationFacadeV3;
    private final ReservationFacadeV4 reservationFacadeV4;

    // ============================================================
    // V0: 외부 기능이 트랜잭션 안에 포함, 데드락 + 동시성 문제
    // ============================================================
    @PostMapping("/v0")
    public ResponseEntity<Void> createV0(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {
        // ServiceV0 내부에서:
        //  - 트랜잭션 범위 안에서 DB + 외부 시스템(메일 등)까지 한 번에 수행
        //  - 데드락, 긴 트랜잭션, 스레드 블로킹 문제 발생 가능
        reservationServiceV0.createReservation(username, request);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V1: saveAndFlush로 데드락은 줄였지만,
    //     여전히 외부 기능이 트랜잭션 안에 포함 + 동시성 문제
    // ============================================================
    @PostMapping("/v1")
    public ResponseEntity<Void> createV1(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {
        // ServiceV1 내부에서:
        //  - saveAndFlush로 데드락 일부 완화
        //  - 그래도 외부 기능이 같은 트랜잭션 안에서 실행됨
        reservationServiceV1.createReservation(username, request);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V2 (+ FacadeV0): 비관락/낙관락으로 동시성은 해결,
    //                 하지만 외부 기능이 아직 트랜잭션에 포함
    // ============================================================
    @PostMapping("/v2/optimistic")
    public ResponseEntity<Void> createV2Optimistic(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) throws InterruptedException {
        // FacadeV0 내부에서:
        //  - ReservationServiceV2 (낙관락) 호출로 동시성 해결
        //  - 그 안/직후에 외부 기능을 같은 흐름에서 실행 (트랜잭션 포함 구조)
        reservationFacadeV0.createReservation(username, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v2/pessimistic")
    public ResponseEntity<Void> createV2Pessimistic(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {
        // FacadeV0 내부에서:
        //  - ReservationServiceV2 (비관락) 호출로 동시성 해결
        //  - 그 안/직후에 외부 기능을 같은 흐름에서 실행 (트랜잭션 포함 구조)
        reservationServiceV2.createReservationPessimistic(username, request);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V3 + FacadeV1: 외부 기능을 트랜잭션에서 분리 (동기 실행)
    // ============================================================
    @PostMapping("/v3/sync/opt")
    public ResponseEntity<Void> createV3SyncExternalOpt(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) throws InterruptedException {
        // FacadeV1:
        //  1) ReservationServiceV3로 예약 생성 (트랜잭션 종료)
        //  2) COMMIT 이후에 외부 기능을 "동기" 호출
        reservationFacadeV1.createReservation(username, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/v3/sync/pes")
    public ResponseEntity<Void> createV3SyncExternalPes(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {

        reservationFacadeV1.createReservationPessimistic(username, request);

        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V3 + FacadeV2: 외부 기능 비동기(@Async) 실행
    // ============================================================
    @PostMapping("/v3/async/opt")
    public ResponseEntity<Void> createV3AsyncExternalOpt(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) throws InterruptedException {
        // FacadeV2:
        //  1) ServiceV3로 예약 생성 (트랜잭션 완료)
        //  2) 별도 스레드풀(mailTaskExecutor 등)에서 @Async 메일 발송
        reservationFacadeV2.createReservation(username, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/v3/async/pes")
    public ResponseEntity<Void> createV3AsyncExternalPes(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {

        reservationFacadeV2.createReservationPessimistic(username, request);

        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V3 + FacadeV3: 외부 기능을 Kafka 기반 비동기 실행
    // ============================================================
    @PostMapping("/v3/kafka/opt")
    public ResponseEntity<Void> createV3KafkaExternalOpt(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) throws InterruptedException{
        // FacadeV3:
        //  1) ServiceV3로 예약 생성
        //  2) ReservationConfirmedEvent 생성 후 Kafka 토픽으로 발행
        //  3) 별도 Email 서비스(컨슈머)가 이벤트를 구독하고 메일 발송
        reservationFacadeV3.createReservation(username, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/v3/kafka/pes")
    public ResponseEntity<Void> createV3KafkaExternalPes(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {

        reservationFacadeV3.createReservationPessimistic(username, request);

        return ResponseEntity.ok().build();
    }

    // ============================================================
    // V4 + FacadeV4: Kafka + Redis 만석 플래그 (핫 슬롯 보호)
    //  - 자리 꽉 찼을 때 Redis full 키 설정
    //  - 취소 시 full 키 삭제
    //  - 키 만료 이후에도 여전히 만석이면 다시 full 키 생성
    // ============================================================
    @PostMapping("/v4/kafka-redis/opt")
    public ResponseEntity<Void> createV4KafkaRedisOpt(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) throws InterruptedException{
        // FacadeV4:
        //  1) Redis full 키로 "이미 만석인지" 빠른 차단
        //  2) ServiceV4 내부에서 Redis 분산락 + DB 예약/슬롯 업데이트
        //  3) 좌석이 꽉 차면 full 키 설정
        //  4) Kafka 이벤트 발행으로 메일 비동기 처리
        reservationFacadeV4.createReservation(username, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/v4/kafka-redis/pes")
    public ResponseEntity<Void> createV4KafkaRedisPes(
            @RequestParam String username,
            @RequestBody ReservationRequestDto request
    ) {

        reservationFacadeV4.createReservationPessimistic(username, request);

        return ResponseEntity.ok().build();
    }

    /**
     * V4 예약 취소 + Redis full 키 처리까지 포함하는 취소 엔드포인트
     *
     * 예: DELETE /api/reservations/test/v4/123?username=test@example.com
     */
    @DeleteMapping("/v4/{reservationId}")
    public ResponseEntity<Void> cancelV4KafkaRedis(
            @RequestParam String username,
            @PathVariable Long reservationId
    ) {
        // FacadeV4 또는 ServiceV4 안에서:
        //  - 예약 취소
        //  - 슬롯 카운트 감소
        //  - 더 이상 만석이 아니라면 Redis full 키 삭제
        //  - 여전히 만석이면 full 키 유지 or 재설정
        reservationServiceV4.cancelReservation(username, reservationId);
        return ResponseEntity.noContent().build();
    }
}