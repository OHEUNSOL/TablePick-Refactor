package com.goorm.tablepick.domain.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSummaryDto;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImprovedRestaurantRedisService {
    private static final String RESTAURANT_CACHE_KEY_PREFIX = "restaurant";
    private static final String NULL_VALUE = "__NULL__";
    private static final String SERVICE_NAME = "ImprovedRestaurantRedisService";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestaurantRepository restaurantRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Cache Penetration 대응:
     *  - 존재하지 않는 restaurantId 에 대해서도 NULL 캐싱
     */
    public RestaurantSummaryDto getRestaurantByIdWithNullCache(Long id) throws JsonProcessingException {
        String key = RESTAURANT_CACHE_KEY_PREFIX + ":" + id;

        // 1. 캐시에서 먼저 조회
        String cachedResult = redisTemplate.opsForValue().get(key);

        // 2. 캐시에 존재하지만 NULL_VALUE가 저장되어 있다면 (데이터가 없음을 의미)
        if (cachedResult != null && cachedResult.equals(NULL_VALUE)) {
            meterRegistry.counter("cache.hit", "service", SERVICE_NAME).increment();
            return null;
        }

        // 3. 캐시에 존재하지만 NULL_VALUE 는 아닌 경우
        if (cachedResult != null) {
            meterRegistry.counter("cache.hit", "service", SERVICE_NAME).increment();
            return objectMapper.readValue(cachedResult, new TypeReference<>() {
            });
        }

        meterRegistry.counter("cache.miss", "service", SERVICE_NAME).increment();

        // 3. 캐시에 아예 없는 경우 DB에서 조회
        Restaurant restaurantFromDb = restaurantRepository.findById(id).orElse(null);
        meterRegistry.counter("db.select", "service", SERVICE_NAME).increment();

        // 4. DB 조회 결과가 null이더라도 캐시에 저장 (짧은 TTL 적용)
        if (restaurantFromDb == null) {
            redisTemplate.opsForValue().set(key, NULL_VALUE, 5, TimeUnit.MINUTES);
            return null;
        }

        RestaurantSummaryDto dto = RestaurantSummaryDto.from(restaurantFromDb);
        String serialized = objectMapper.writeValueAsString(dto);
        redisTemplate.opsForValue().set(key, serialized, 30, TimeUnit.MINUTES);
        return dto;
    }
}