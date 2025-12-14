package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.entity.RestaurantOperatingHour;
import com.goorm.tablepick.domain.restaurant.enums.DayOfWeek;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantOperatingHourRepository;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
@Slf4j
public class ReservationSlotGeneratorTest {
    private final RestaurantRepository restaurantRepository;

    /**
     * 오늘 기준 targetDate(예: 일주일 뒤)에 대해
     * 모든 식당의 예약 슬롯(08:00 ~ 22:00, 1시간 간격 15타임)을 메모리에서 생성만 한다.
     */
    public List<ReservationSlot> generateSlotsForWeek() {
        LocalDate targetDate = LocalDate.now().plusDays(8); // 예시(11/3 → 11/11)에 맞춰 8일 뒤
        List<Restaurant> restaurants = restaurantRepository.findAll();

        // 08:00 ~ 22:00, 1시간 간격 15타임
        List<LocalTime> times = IntStream.range(0, 15)
                .mapToObj(i -> LocalTime.of(8, 0).plusHours(i))
                .toList();

        List<ReservationSlot> result = new ArrayList<>();

        for (Restaurant restaurant : restaurants) {
            for (LocalTime time : times) {
                ReservationSlot slot = ReservationSlot.builder()
                        .restaurant(restaurant)
                        .date(targetDate)
                        .time(time)
                        .count(0L)
                        .build();
                result.add(slot);
            }
        }

        log.info("[SlotGenerate] targetDate={}, restaurants={}, slots={}",
                targetDate, restaurants.size(), result.size());

        return result;
    }
}