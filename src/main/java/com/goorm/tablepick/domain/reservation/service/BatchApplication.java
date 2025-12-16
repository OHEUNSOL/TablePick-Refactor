package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.monitoring.BatchContext;
import com.goorm.tablepick.domain.reservation.monitoring.BatchContextHolder;
import com.goorm.tablepick.domain.reservation.monitoring.BatchName;
import com.goorm.tablepick.domain.reservation.service.ReservationSlotService.ReservationSlotServiceV0;
import com.goorm.tablepick.domain.reservation.service.ReservationSlotService.ReservationSlotServiceV1;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class BatchApplication {

    private final ReservationSlotGenerator reservationSlotGenerator;
    private final ReservationSlotServiceV0 reservationSlotServiceV0;
    private final ReservationSlotServiceV1 reservationSlotServiceV1;
    private final EntityManagerFactory entityManagerFactory;

    private static final int JDBC_BATCH_SIZE = 3000;
    private static final int limitCount = 10000;

    /**
     * V0: JPA saveAll 버전
     *  - 실행 시간(ms)과 실제 Hibernate 쿼리 수를 로그로 출력
     */
    public void runInsertV0() {
        List<ReservationSlot> slots = reservationSlotGenerator.generateSlotsForWeek(limitCount);

        // Hibernate Statistics 준비
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        long startQueryCount = stats.getPrepareStatementCount();

        long startTime = System.currentTimeMillis();

        BatchContextHolder.initContext(new BatchContext(BatchName.BULK_INSERT));
        try {
            reservationSlotServiceV0.bulkInsert(slots);
        } finally {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            long endQueryCount = stats.getPrepareStatementCount();
            long executedQueries = endQueryCount - startQueryCount;


            BatchContext ctx = BatchContextHolder.getContext();
            if (ctx != null) {
                ctx.log();
            }
            BatchContextHolder.clear();
        }
    }

    /**
     * V1: JdbcTemplate Batch 버전
     *  - 실행 시간(ms)과 '예상' 쿼리 수를 로그로 출력
     *    (slots / JDBC_BATCH_SIZE 로 배치 개수 추정)
     */
    public void runInsertV1() {
        List<ReservationSlot> slots = reservationSlotGenerator.generateSlotsForWeek(limitCount);

        long startTime = System.currentTimeMillis();

        BatchContextHolder.initContext(new BatchContext(BatchName.BULK_INSERT));
        try {
            reservationSlotServiceV1.bulkInsert(slots);
        } finally {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;

            // JdbcTemplate 쪽은 Hibernate Statistics 로 안 잡히니까,
            // 배치 크기 기준으로 "대략 몇 번의 INSERT 쿼리가 나갔는지" 추정
            long estimatedBatchQueries =
                    (slots.size() + JDBC_BATCH_SIZE - 1) / JDBC_BATCH_SIZE;


            BatchContext ctx = BatchContextHolder.getContext();
            if (ctx != null) {
                ctx.log();
            }
            BatchContextHolder.clear();
        }
    }

    /**
     * Delete V0 (JPA)
     *  - deleteAllInBatch() 같은 방식일 경우 실제 쿼리 수도 함께 로그
     */
    public void runDeleteV0() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        long startQueryCount = stats.getPrepareStatementCount();

        long startTime = System.currentTimeMillis();

        BatchContextHolder.initContext(new BatchContext(BatchName.BULK_DELETE));
        try {
            reservationSlotServiceV0.bulkDelete();
        } finally {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;

            long endQueryCount = stats.getPrepareStatementCount();
            long executedQueries = endQueryCount - startQueryCount;

            BatchContext ctx = BatchContextHolder.getContext();
            if (ctx != null) {
                ctx.log();
            }
            BatchContextHolder.clear();
        }
    }

    /**
     * Delete V1 (JdbcTemplate)
     *  - 보통 단일 DELETE 쿼리라고 가정해서 1회로 로그
     */
    public void runDeleteV1() {
        long startTime = System.currentTimeMillis();

        BatchContextHolder.initContext(new BatchContext(BatchName.BULK_DELETE));
        try {
            reservationSlotServiceV1.bulkDelete();
        } finally {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;

            BatchContext ctx = BatchContextHolder.getContext();
            if (ctx != null) {
                ctx.log();
            }
            BatchContextHolder.clear();
        }
    }
}