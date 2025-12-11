package com.goorm.tablepick.domain.restaurant.entity;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.tag.entity.RestaurantTag;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String restaurantPhoneNumber;
    
    private String address;
    
    private Double xcoordinate;
    
    private Double ycoordinate;
    
    private int maxCapacity;

    private String mainImageUrl;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<ReservationSlot> reservationSlots = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<Menu> menus = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<RestaurantImage> restaurantImages = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<RestaurantOperatingHour> restaurantOperatingHours = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<RestaurantTag> restaurantTags = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "restaurant_category_id")
    private RestaurantCategory restaurantCategory;
    
}
