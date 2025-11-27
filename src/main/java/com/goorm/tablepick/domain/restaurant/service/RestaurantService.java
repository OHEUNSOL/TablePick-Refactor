package com.goorm.tablepick.domain.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.restaurant.dto.request.RestaurantSearchRequestDto;
import com.goorm.tablepick.domain.restaurant.dto.response.*;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantCategoryRepository;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
@Transactional(readOnly = true)
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;

    @Autowired
    ObjectMapper mapper;

    public Page<RestaurantResponseDto> getAllRestaurants(Pageable pageable, Member member) {
        Page<Restaurant> restaurantPage;

        restaurantPage = restaurantRepository.findPopularRestaurants(pageable);

        return restaurantPage.map(restaurant -> {

            return new RestaurantResponseDto(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getRestaurantCategory().getName(),
                    restaurant.getAddress(),
                    restaurant.getRestaurantImages().isEmpty() ? null
                            : restaurant.getRestaurantImages().getFirst().getImageUrl()
            );
        });
    }


    private List<String> deserializeTags(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse board tags JSON: {}", json, e);
            return List.of();
        }
    }

    public Page<RestaurantSearchResponseDto> searchRestaurants(RestaurantSearchRequestDto requestDto, Pageable pageable) {

        Page<Restaurant> restaurants = restaurantRepository.findRestaurantsByComplexCondition_Problematic(
                requestDto.getCategoryId(),
                requestDto.getReservationDate(),
                requestDto.getReservationTime(),
                pageable
        );

        // (N+1은 여기서 발생하지만, 일단 DTO에서 이미지 필드를 빼서 무시)
        return restaurants.map(this::toRestaurantResponse);
    }

    private RestaurantSearchResponseDto toRestaurantResponse(Restaurant restaurant) {
        // (이전 대화에서 이미지 필드는 DTO에서 빼기로 함)
        return RestaurantSearchResponseDto.builder()
                .id(restaurant.getId())
                .address(restaurant.getAddress())
                .restaurantCategory(restaurant.getRestaurantCategory().getName())
                .build();
    }
}
