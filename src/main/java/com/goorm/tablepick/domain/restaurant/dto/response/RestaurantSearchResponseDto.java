package com.goorm.tablepick.domain.restaurant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
public class RestaurantSearchResponseDto {

    @Schema(description = "식당 아이디", example = "1")
    private Long id;

    @Schema(description = "식당 이름", example = "더미 식당")
    private String name;

    @Schema(description = "식당 주소", example = "서울특별시 강남구 강남대로 11")
    private String address;

    @Schema(description = "식당 카테고리", example = "한식")
    private String restaurantCategory;

    @Builder
    public RestaurantSearchResponseDto(Long id, String name, String address, String restaurantCategory) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.restaurantCategory = restaurantCategory;
    }
}
