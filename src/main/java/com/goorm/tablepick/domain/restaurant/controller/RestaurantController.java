package com.goorm.tablepick.domain.restaurant.controller;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.restaurant.dto.request.RestaurantSearchRequestDto;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantResponseDto;
import com.goorm.tablepick.domain.restaurant.dto.response.RestaurantSearchResponseDto;
import com.goorm.tablepick.domain.restaurant.service.RestaurantService;
import com.goorm.tablepick.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService restaurantService;

    @GetMapping("/all")
    @Operation(summary = "전체 식당 목록 조회", description = "전체 식당 목록을 조회합니다.")
    public ResponseEntity<Page<RestaurantResponseDto>> getAllRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = (userDetails != null) ? userDetails.getMember() : null;
        Pageable pageable = PageRequest.of(page, 4);
        return ResponseEntity.ok(restaurantService.getAllRestaurants(pageable, member));
    }

    @GetMapping("/complex-search")
    @Operation(summary = "식당 검색", description = "카테고리, 예약 날짜, 예약 시간으로 예약 가능한 식당을 검색합니다.")
    public ResponseEntity<Page<RestaurantSearchResponseDto>> searchRestaurantsV1(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "reservationDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationDate,
            @RequestParam(value = "reservationTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime reservationTime,
            Pageable pageable) {

        RestaurantSearchRequestDto requestDto = RestaurantSearchRequestDto.builder()
                .categoryId(categoryId)
                .reservationDate(reservationDate)
                .reservationTime(reservationTime)
                .build();

        Page<RestaurantSearchResponseDto> response = restaurantService.searchRestaurants(requestDto, pageable);
        return ResponseEntity.ok(response);
    }
}