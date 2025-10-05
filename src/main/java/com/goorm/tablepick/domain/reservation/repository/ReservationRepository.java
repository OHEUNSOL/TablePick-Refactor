package com.goorm.tablepick.domain.reservation.repository;

import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByReservationSlot(ReservationSlot reservationSlot);

    @Query("SELECT r FROM Reservation r WHERE r.member.email = :username AND r.reservationStatus != 'CANCELLED'")
    List<Reservation> findAllByMemberEmail(@Param(value = "username") String username);

    // 특정 시간 범위 내의 예약을 조회하는 메서드 (알림 스케줄링용)
    // ReservationSlot의 date와 time을 조합하여 시간 범위를 확인
    @Query("SELECT r FROM Reservation r " +
            "JOIN r.reservationSlot rs " +
            "WHERE CONCAT(rs.date, ' ', rs.time) BETWEEN :startTime AND :endTime")
    List<Reservation> findByReservationDateTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 회원 ID로 예약을 조회하는 메서드
    List<Reservation> findByMemberId(Long memberId);

    // ID로 예약 조회 (Optional이 아닌 직접 Reservation 반환)
    // 기본 findById는 Optional<Reservation>을 반환하므로 추가
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Reservation getReservationById(@Param("id") Long id);

    // 특정 날짜 범위 내의 대기 중인 예약을 조회하는 메서드 (pending or null)
    @Query("SELECT r FROM Reservation r " +
            "JOIN r.reservationSlot rs " +
            "WHERE (r.reservationStatus = 'PENDING' OR r.reservationStatus IS NULL) " +
            "AND rs.date BETWEEN :startDate AND :endDate")
    List<Reservation> findPendingReservationsBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    long countByReservationSlot(ReservationSlot updatedSlot);

    // 특정 시간 이전에 생성된 외부 ID 미할당 참가자 조회
    @Query("SELECT r FROM Reservation r WHERE (r.paymentStatus = :status) AND r.createdAt <= :now")
    List<Reservation> findByPaymentStatusEquals(String status, LocalDateTime now);
}
