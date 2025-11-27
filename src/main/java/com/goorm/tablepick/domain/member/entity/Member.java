package com.goorm.tablepick.domain.member.entity;

import com.goorm.tablepick.domain.member.enums.AccountRole;
import com.goorm.tablepick.domain.member.enums.Gender;
import com.goorm.tablepick.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 30, nullable = false, unique = true)
    private String email;

    @Column(length = 30, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    private LocalDate birthdate;
    
    private String phoneNumber;
    
    private String profileImage;
    
    private Boolean isMemberDeleted;

    private String provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private AccountRole roles;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

}
