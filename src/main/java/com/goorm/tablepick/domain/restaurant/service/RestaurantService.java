package com.goorm.tablepick.domain.restaurant.service;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.restaurant.dto.request.RestaurantSearchRequestDto;
import com.goorm.tablepick.domain.restaurant.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantService {

    Page<RestaurantResponseDto> getAllRestaurants(Pageable pageable, Member member);

    List<CategoryResponseDto> getCategoryList();

}
