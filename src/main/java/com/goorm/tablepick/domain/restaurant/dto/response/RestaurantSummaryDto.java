package com.goorm.tablepick.domain.restaurant.dto.response;

import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.tag.entity.RestaurantTag;
import com.goorm.tablepick.domain.tag.entity.Tag;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder
public class RestaurantSummaryDto {
    private Long id;
    private String name;
    private String categoryName;
    private String phoneNumber;
    private String address;
    private int maxCapacity;
    private List<String> tags;

    public static RestaurantSummaryDto from(Restaurant restaurant) {
        // 태그 이름 리스트 추출
        List<String> tagNames =
                restaurant.getRestaurantTags() == null
                        ? Collections.emptyList()
                        : restaurant.getRestaurantTags().stream()
                        .map(RestaurantTag::getTag)
                        .filter(Objects::nonNull)
                        .map(Tag::getName)
                        .collect(Collectors.toList());

        return RestaurantSummaryDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .categoryName(
                        restaurant.getRestaurantCategory() != null
                                ? restaurant.getRestaurantCategory().getName()
                                : null
                )
                .phoneNumber(restaurant.getRestaurantPhoneNumber())
                .address(restaurant.getAddress())
                .maxCapacity(restaurant.getMaxCapacity())
                .tags(tagNames)
                .build();
    }
}