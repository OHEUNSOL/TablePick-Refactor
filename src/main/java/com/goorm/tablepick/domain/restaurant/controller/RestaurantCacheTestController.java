package com.goorm.tablepick.domain.restaurant.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSummaryDto;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import com.goorm.tablepick.domain.restaurant.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantCacheTestController {

    private final RestaurantService restaurantService;
    private final RestaurantRedisService restaurantRedisService;
    private final ImprovedCacheAvalancheService improvedCacheAvalancheService;
    private final ImprovedHotKeyService improvedHotKeyService;
    private final ImprovedRestaurantRedisService improvedRestaurantRedisService;

    // =========================
    // v0 : 캐시 미적용 (DB 직통)
    // =========================
    @GetMapping("/v0/{restaurantId}")
    public ResponseEntity<RestaurantSummaryDto> getRestaurantV0(@PathVariable Long restaurantId) {

        RestaurantSummaryDto dto = restaurantService.getRestaurantById(restaurantId);

        return ResponseEntity.ok(dto);
    }

    // =========================
    // v1 : 기본 Redis 캐시
    // =========================
    @GetMapping("/v1/{restaurantId}")
    public ResponseEntity<RestaurantSummaryDto> getRestaurantV1(
            @PathVariable Long restaurantId
    ) throws JsonProcessingException {

        RestaurantSummaryDto dto = restaurantRedisService.getRestaurantById(restaurantId);

        return ResponseEntity.ok(dto);
    }

    // =========================
    // v2 : null 캐싱
    // =========================
    @GetMapping("/v2/{restaurantId}")
    public ResponseEntity<RestaurantSummaryDto> getRestaurantV2(
            @PathVariable Long restaurantId
    ) throws JsonProcessingException {

        RestaurantSummaryDto dto = improvedRestaurantRedisService.getRestaurantByIdWithNullCache(restaurantId);

        return ResponseEntity.ok(dto);
    }

    // =========================
    // v3 : TTL + Jitter / null 캐싱
    // =========================
    @GetMapping("/v3/{restaurantId}")
    public ResponseEntity<RestaurantSummaryDto> getRestaurantV3(
            @PathVariable Long restaurantId
    ) throws JsonProcessingException {

        RestaurantSummaryDto dto = improvedCacheAvalancheService.getRestaurantByIdWithJitter(restaurantId);

        return ResponseEntity.ok(dto);
    }

    // =========================
    // v4 : Hot Key + 분산 락
    // =========================
    @GetMapping("/v4/{restaurantId}")
    public ResponseEntity<RestaurantSummaryDto> getRestaurantV4(
            @PathVariable Long restaurantId
    ) throws JsonProcessingException {
        RestaurantSummaryDto dto = improvedHotKeyService.getRestaurantByIdWithLock(restaurantId);

        return ResponseEntity.ok(dto);
    }
}