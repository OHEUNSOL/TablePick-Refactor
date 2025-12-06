package com.goorm.tablepick.domain.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSummaryDto;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class RestaurantRedisService {
    private static final String SERVICE_NAME = "RestaurantRedisService";
    private static final String CACHE_KEY_PREFIX = "restaurant";

    private final RestaurantRepository restaurantRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     *   원본(문제 있는) 버전
     *  - 존재하지 않는 ID는 캐시에 아예 안 넣음 → Cache Penetration 가능
     *  - 단순히 캐시 미스면 매번 DB 조회
     */
    public RestaurantSummaryDto getRestaurantById(Long id) throws JsonProcessingException {
        String key = CACHE_KEY_PREFIX + ":" + id;

        // 1. 캐시 조회
        String cachedResult = redisTemplate.opsForValue().get(key);

        // 2. 캐시에 있으면 바로 반환
        if (cachedResult != null) {
            meterRegistry.counter("cache.hit", "service", SERVICE_NAME).increment();
            return objectMapper.readValue(cachedResult, new TypeReference<>() {});
        }

        meterRegistry.counter("cache.miss", "service", SERVICE_NAME).increment();

        // 3. DB 조회
        Restaurant restaurantFromDb = restaurantRepository.findById(id).orElse(null);
        meterRegistry.counter("db.select", "service", SERVICE_NAME).increment();

        // 4. DB 에서 찾은 경우에만 캐시에 저장
        if (restaurantFromDb != null) {
            RestaurantSummaryDto dto = RestaurantSummaryDto.from(restaurantFromDb);
            String serialized = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, serialized, 1, TimeUnit.HOURS);
            return dto;
        }

        // 못 찾으면 null
        return null;
    }
}