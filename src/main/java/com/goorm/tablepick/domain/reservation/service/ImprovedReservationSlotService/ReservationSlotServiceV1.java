package com.goorm.tablepick.domain.reservation.service.ImprovedReservationSlotService;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import com.goorm.tablepick.domain.reservation.service.ReservationSlotGenerator;
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

    private final ReservationSlotGenerator slotGenerator;
    private final ReservationSlotRepository slotRepository;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void bulkInsert(List<ReservationSlot> reservationSlots) {
        String sql = """
                INSERT INTO reservation_slot (date, time, count, restaurant_id)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE count = count
                """;

        jdbcTemplate.batchUpdate(sql,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ReservationSlot slot = reservationSlots.get(i);
                        ps.setDate(1, java.sql.Date.valueOf(slot.getDate()));
                        ps.setTime(2, java.sql.Time.valueOf(slot.getTime()));
                        ps.setLong(3, slot.getCount());
                        ps.setLong(4, slot.getRestaurant().getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return reservationSlots.size();
                    }
                });

    }


    @Transactional
    public void bulkDelete() {
        entityManager.flush(); // 영속성 컨텍스트의 변경 내용을 데이터베이스에 반영
        slotRepository.deleteAllInBatch(); // Batch Delete 쿼리 실행
        entityManager.clear(); // 영속성 컨텍스트 초기화
    }
}
