package com.goorm.tablepick.domain.restaurant.repository;

import com.goorm.tablepick.domain.restaurant.entity.RestaurantOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantOperatingHourRepository extends JpaRepository<RestaurantOperatingHour, Long> {

    List<RestaurantOperatingHour> findAllByRestaurantId(Long id);

    List<RestaurantOperatingHour> findByRestaurantId(Long id);
}
