package com.goorm.tablepick.domain.restaurant.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class RestaurantSearchRequestDto {

    private Long categoryId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;

}