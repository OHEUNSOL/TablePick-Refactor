package com.goorm.tablepick.domain.restaurant.repository;

import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSearchResponse;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query("""
            SELECT r FROM Restaurant r
            LEFT JOIN ReservationSlot slot ON r.id = slot.restaurant.id
            LEFT JOIN Reservation res ON slot.id = res.reservationSlot.id
            WHERE r.restaurantCategory IS NOT NULL
              AND SIZE(r.restaurantImages) > 0
            GROUP BY r.id
            ORDER BY COUNT(r.id) DESC, r.id
            """)
    Page<Restaurant> findPopularRestaurants(Pageable pageable);


    // RestaurantRepository.java
    @Query(value = "SELECT r FROM Restaurant r WHERE r.id IN :ids")
    Page<Restaurant> findRestaurantsByIdsInOrder(@Param("ids") List<Long> ids, Pageable pageable);

    boolean existsByName(String newName);

    List<Restaurant> findByNameIgnoreCase(String newName);

    @Query("SELECT r FROM Restaurant r " +
            "JOIN r.restaurantCategory rc " +
            "WHERE " +
            // 1. 카테고리
            "(:categoryId IS NULL OR rc.id = :categoryId) " +
            // 2. 예약 시간 (EXISTS 서브쿼리)
            "AND (" +
            "    (:reservationDate IS NULL OR :reservationTime IS NULL) " +
            "    OR EXISTS (SELECT s.id FROM ReservationSlot s " +
            "                 WHERE s.restaurant = r " +
            "                 AND s.date = :reservationDate " +
            "                 AND s.time = :reservationTime " +
            "                 AND s.count > 0) " +
            ")"
    )
    Page<Restaurant> findRestaurantsByComplexCondition_Problematic(
            @Param("categoryId") Long categoryId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("reservationTime") LocalTime reservationTime,
            Pageable pageable);
}
