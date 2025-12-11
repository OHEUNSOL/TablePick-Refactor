package com.goorm.tablepick.domain.restaurant.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Builder
    public RestaurantImage(String imageUrl, Restaurant restaurant,  Integer sortOrder) {
        this.imageUrl = imageUrl;
        this.restaurant = restaurant;
        this.sortOrder = sortOrder;
    }
}
