package com.goorm.tablepick.domain.reservation.entity;

import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private LocalTime time;

    private Long count;

    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "reservationSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    @Builder
    public ReservationSlot(LocalDate date, LocalTime time, Long count, Restaurant restaurant) {
        this.date = date;
        this.time = time;
        this.count = count;
        this.restaurant = restaurant;
    }

    // 예약 날짜와 시간을 합쳐서 LocalDateTime으로 반환
    public LocalDateTime getDateTime() {
        if (date != null && time != null) {
            return LocalDateTime.of(date, time);
        }
        return null;
    }

    public void setCount(long count) {
        this.count = count;
    }

}
