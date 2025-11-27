package com.goorm.tablepick.domain.tag.repository;

import com.goorm.tablepick.domain.tag.entity.RestaurantTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantTagRepository extends JpaRepository<RestaurantTag, Long> {
}
