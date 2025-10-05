package com.goorm.tablepick.domain.restaurant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantSearchResponse {

    @Schema(description = "식당 아이디", example = "1")
    private Long id;

    @Schema(description = "식당 이름", example = "더미 식당")
    private String name;

    @Schema(description = "식당 주소", example = "서울특별시 강남구 강남대로 11")
    private String address;

    @Schema(description = "식당 카테고리", example = "한식")
    private String restaurantCategory;

    @Schema(description = "식당 이미지", example = "url")
    private String restaurantImage;

    private String boardTagsJson;
}
