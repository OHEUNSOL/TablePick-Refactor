package com.goorm.tablepick.domain.member.dto;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.entity.MemberTag;
import com.goorm.tablepick.domain.member.enums.Gender;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public static MemberResponseDto toDto(Member member) {
        List<Long>  memberTagIds = new ArrayList<>();
        List<MemberTag> memberTags = member.getMemberTags();
        if(memberTags != null && !memberTags.isEmpty()){
            memberTagIds.addAll(memberTags.stream().map(memberTag -> {return memberTag.getTag().getId();}).toList());
        }

        return MemberResponseDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .gender(member.getGender())
                .birthdate(member.getBirthdate())
                .phoneNumber(member.getPhoneNumber())
                .profileImage(member.getProfileImage())
                .provider(member.getProvider())
                .providerId(member.getProviderId())
                .createAt(member.getCreatedAt())
                .memberTagIds(memberTagIds)
                .build();
    }
}
