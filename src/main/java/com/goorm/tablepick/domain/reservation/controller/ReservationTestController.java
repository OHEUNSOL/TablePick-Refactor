package com.goorm.tablepick.domain.reservation.controller;

import com.goorm.tablepick.domain.reservation.dto.request.ReservationRequestDto;
import com.goorm.tablepick.domain.reservation.facade.V0.CreateReservationTestFacadeV0;
import com.goorm.tablepick.domain.reservation.facade.V0.OptimisticLockFacadeV0;
import com.goorm.tablepick.domain.reservation.facade.V1.OptimisticLockFacadeV1;
import com.goorm.tablepick.domain.reservation.facade.V2.OptimisticLockFacadeV2;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationTestController {
    private final CreateReservationTestFacadeV0 createReservationTestFacadeV0;
    private final OptimisticLockFacadeV0 optimisticLockFacadeV0;
    private final OptimisticLockFacadeV1 optimisticLockFacadeV1;
    private final OptimisticLockFacadeV2 optimisticLockFacadeV2;

    @PostMapping("/test/v0/optimistic/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservationOptimisticV0(@PathVariable Long memberId,
                                                            @RequestBody @Valid ReservationRequestDto request) {

        try {
            optimisticLockFacadeV0.createReservationWithOptimisticLock(memberId, request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test/v0/pessimistic/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservationPessimistic(@PathVariable Long memberId,
                                                             @RequestBody @Valid ReservationRequestDto request) {

        createReservationTestFacadeV0.createReservationPessimistic(memberId, request);
        log.info("예약 생성 성공");
        return ResponseEntity.ok().build();
    }

    // 모놀리식 (동기), 트랜잭션 분리 전
    @PostMapping("/test/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservation(@PathVariable Long memberId,
                                                    @RequestBody @Valid ReservationRequestDto request) {

        try {
            optimisticLockFacadeV2.createReservationWithOptimisticLock(memberId, request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    // 모놀리식 (동기), 트랜잭션 분리 후
    @PostMapping("/test/v0/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservationV0(@PathVariable Long memberId,
                                                              @RequestBody @Valid ReservationRequestDto request) {

        try {
            optimisticLockFacadeV0.createReservationWithOptimisticLock(memberId, request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    // 결제 서버 분리 (동기)
    @PostMapping("/test/v1/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservationV1(@PathVariable Long memberId,
                                                              @RequestBody @Valid ReservationRequestDto request) {

        try {
            optimisticLockFacadeV1.createReservationWithOptimisticLock(memberId, request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    // 결제 서버 분리 (비동기)
    @PostMapping("/test/v2/{memberId}")
    @Operation(summary = "예약 생성", description = "식당, 유저, 예약 시간 정보를 기반으로 예약을 생성합니다.")
    public ResponseEntity<Void> createReservationV2(@PathVariable Long memberId,
                                                              @RequestBody @Valid ReservationRequestDto request) {

        try {
            optimisticLockFacadeV2.createReservationWithOptimisticLock(memberId, request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }



}
