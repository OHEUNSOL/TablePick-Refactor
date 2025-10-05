package com.goorm.tablepick.domain.restaurant.dto.response;


import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class RestaurantListResponseDto {
    @Schema(description = "식당 아이디", example = "1")
    private Long id;
    @Schema(description = "식당 이름", example = "더미 식당")
    private String name;
    @Schema(description = "식당 주소", example = "서울특별시 강남구 강남대로 11")
    private String address;
    @Schema(description = "식당 번호", example = "023456789")
    private String restaurantPhoneNumber;
    @Schema(description = "식당 카테고리", example = "한식")
    private String restaurantCategory;
    @Schema(description = "식당 이미지", example = "url")
    private String restaurantImage;
    @Schema(description = "식당 태그", example = "역이랑 가까워요")
    private List<String> restaurantTags;

    public static RestaurantListResponseDto toDto(Restaurant restaurant) {
        return RestaurantListResponseDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .restaurantPhoneNumber(restaurant.getRestaurantPhoneNumber())
                .address(restaurant.getAddress())
                .restaurantCategory(restaurant.getRestaurantCategory().getName())
                .restaurantImage(
                        restaurant.getRestaurantImages() != null && !restaurant.getRestaurantImages().isEmpty()
                                ? restaurant.getRestaurantImages().get(0).getImageUrl()
                                : null
                )
                .build();
    }

}
