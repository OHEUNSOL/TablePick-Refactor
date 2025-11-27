package com.goorm.tablepick.domain.member.dto;

import com.goorm.tablepick.domain.member.enums.Gender;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class MemberResponseDto {
    private Long id;
    private String nickname;
    private String email;
    private Gender gender;
    private LocalDate birthdate;
    private String phoneNumber;
    private String profileImage;
    private String provider;
    private String providerId;
    private LocalDateTime createAt;
    private List<Long> memberTagIds;

}
