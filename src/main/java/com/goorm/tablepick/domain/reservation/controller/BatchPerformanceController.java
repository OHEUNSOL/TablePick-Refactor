package com.goorm.tablepick.domain.reservation.controller;

import com.goorm.tablepick.domain.reservation.service.BatchApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test/batch")
@RequiredArgsConstructor
public class BatchPerformanceController {

    private final BatchApplication batchApplication;

    // ==========================================
    // 1. INSERT 테스트 (데이터 생성)
    // ==========================================

    /**
     * [Version 0] JPA saveAll 실행
     * - Hibernate Statistics로 쿼리 수 측정
     * - Timer로 실행 시간 측정
     */
    @PostMapping("/insert/v0")
    public String testInsertV0() {
        log.info("Starting Batch Insert V0 (JPA)...");
        batchApplication.runInsertV0();
        return "Insert V0 (JPA) Completed.";
    }

    /**
     * [Version 1] JDBC Template Batch 실행
     * - 계산된 공식으로 쿼리 수 기록
     * - Timer로 실행 시간 측정
     */
    @PostMapping("/insert/v1")
    public String testInsertV1() {
        log.info("Starting Batch Insert V1 (JDBC Batch)...");
        batchApplication.runInsertV1();
        return "Insert V1 (JDBC Batch) Completed.";
    }

    // ==========================================
    // 2. DELETE 테스트 (데이터 초기화 및 삭제 성능)
    // ==========================================

    /**
     * [Version 0] JPA deleteAll (혹은 deleteAllInBatch)
     */
    @DeleteMapping("/delete/v0")
    public String testDeleteV0() {
        log.info("Starting Batch Delete V0 (JPA)...");
        batchApplication.runDeleteV0();
        return "Delete V0 (JPA) Completed.";
    }

    /**
     * [Version 1] JDBC Template 기반 삭제
     */
    @DeleteMapping("/delete/v1")
    public String testDeleteV1() {
        log.info("Starting Batch Delete V1 (JDBC)...");
        batchApplication.runDeleteV1();
        return "Delete V1 (JDBC) Completed.";
    }
}