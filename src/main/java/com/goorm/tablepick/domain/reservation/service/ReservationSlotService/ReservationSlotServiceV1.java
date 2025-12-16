package com.goorm.tablepick.domain.reservation.service.ReservationSlotService;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.monitoring.BatchContext;
import com.goorm.tablepick.domain.reservation.monitoring.BatchContextHolder;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ReservationSlotServiceV1 {

    private final ReservationSlotRepository slotRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final int BATCH_SIZE = 3000;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void bulkInsert(List<ReservationSlot> slots) {
        String sql = """
                INSERT INTO reservation_slot (date, time, count, restaurant_id, version)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE count = count
                """;

        for (int i = 0; i < slots.size(); i += BATCH_SIZE) {
            List<ReservationSlot> batch = slots.subList(
                    i, Math.min(i + BATCH_SIZE, slots.size())
            );

            BatchContext ctx = BatchContextHolder.getContext();
            if (ctx != null) {
                ctx.incrementQueryCount(sql);   // sql 안에 INSERT 있어서 QueryType.INSERT 로 잡힘
            }


            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int j) throws SQLException {
                    ReservationSlot slot = batch.get(j);
                    ps.setDate(1, java.sql.Date.valueOf(slot.getDate()));
                    ps.setTime(2, java.sql.Time.valueOf(slot.getTime()));
                    ps.setLong(3, slot.getCount());
                    ps.setLong(4, slot.getRestaurant().getId());
                    ps.setLong(5, 0L);
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        }

    }


    @Transactional
    public void bulkDelete() {
        entityManager.flush(); // 영속성 컨텍스트의 변경 내용을 데이터베이스에 반영
        slotRepository.deleteAllInBatch(); // Batch Delete 쿼리 실행
        entityManager.clear(); // 영속성 컨텍스트 초기화
    }
}
