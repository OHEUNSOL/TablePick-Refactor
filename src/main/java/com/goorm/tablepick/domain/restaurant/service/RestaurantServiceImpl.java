package com.goorm.tablepick.domain.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.restaurant.dto.request.RestaurantSearchRequestDto;
import com.goorm.tablepick.domain.restaurant.dto.response.*;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.entity.RestaurantCategory;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantErrorCode;
import com.goorm.tablepick.domain.restaurant.exception.RestaurantException;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantCategoryRepository;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;

    @Autowired
    ObjectMapper mapper;

    @Override
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

    @Override
    public List<CategoryResponseDto> getCategoryList() {
        List<RestaurantCategory> categoryList = restaurantCategoryRepository.findAll();
        return categoryList.stream()
                .map(CategoryResponseDto::toDto)
                .collect(Collectors.toList());
    }


    private RestaurantSearchResponseDto toResponseDto(RestaurantSearchResponse proj) {
        List<String> tags = Optional.ofNullable(proj.getBoardTagsJson())
                .map(this::deserializeTags)
                .orElseGet(List::of);

        return RestaurantSearchResponseDto.builder()
                .id(proj.getId())
                .name(proj.getName())
                .address(proj.getAddress())
                .restaurantCategory(proj.getRestaurantCategory())
                .restaurantImage(proj.getRestaurantImage())
                .boardTags(tags)
                .build();
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


}
