package com.goorm.tablepick.domain.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSummaryDto;
import com.goorm.tablepick.domain.restaurant.entity.Restaurant;
import com.goorm.tablepick.domain.restaurant.repository.RestaurantRepository;
import com.goorm.tablepick.global.util.DbCallChecker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImprovedCacheAvalancheService {
    private static final String CACHE_KEY_PREFIX = "restaurant";
    private static final String SERVICE_NAME = "ImprovedCacheAvalancheService";
    private static final String NULL_VALUE = "__NULL__"; // null을 대체할 객체
    // 기본 TTL 60초 + Jitter(0~30초 랜덤)
    private static final long TTL_BASE = 60L;
    private static final long JITTER_RANGE = 30L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestaurantRepository restaurantRepository;
    private final MeterRegistry meterRegistry;
    private final DbCallChecker dbCallChecker = new DbCallChecker("JITTER 적용");


    public RestaurantSummaryDto getRestaurantByIdWithJitter(Long id) throws JsonProcessingException {
        String key = CACHE_KEY_PREFIX + ":" + id;

        // 1. 캐시 조회
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
        dbCallChecker.incrementDbSelectCount();

        // 4. DB 조회 결과가 null이더라도 캐시에 저장 (짧은 TTL 적용)
        if (restaurantFromDb == null) {
            // jitter 적용된 TTL로 저장
            // ms 단위로 저장
            long ttl = TTL_BASE + ThreadLocalRandom.current().nextLong(JITTER_RANGE);
            redisTemplate.opsForValue().set(key, NULL_VALUE, ttl, TimeUnit.MILLISECONDS);
            return null;
        }

        RestaurantSummaryDto dto = RestaurantSummaryDto.from(restaurantFromDb);
        String serialized = objectMapper.writeValueAsString(dto);
        // jitter 적용된 TTL로 저장
        // ms 단위로 저장
        long ttl = TTL_BASE + ThreadLocalRandom.current().nextLong(JITTER_RANGE);
        redisTemplate.opsForValue().set(key, serialized, ttl, TimeUnit.MILLISECONDS);
        return dto;

    }
}