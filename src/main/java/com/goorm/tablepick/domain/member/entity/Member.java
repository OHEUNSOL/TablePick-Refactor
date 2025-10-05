package com.goorm.tablepick.domain.member.entity;

import com.goorm.tablepick.domain.member.dto.MemberAddtionalInfoRequestDto;
import com.goorm.tablepick.domain.member.dto.MemberUpdateRequestDto;
import com.goorm.tablepick.domain.member.enums.AccountRole;
import com.goorm.tablepick.domain.member.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nickname;
    
    @Column(length = 30, nullable = false, unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    private LocalDate birthdate;
    
    private String phoneNumber;
    
    private String profileImage;
    
    private Boolean isMemberDeleted;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberTag> memberTags;
    
    @Enumerated(EnumType.STRING)
    private AccountRole roles;
    
    private String provider;
    
    private String providerId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // FCM 토큰 필드 추가
    @Column(length = 255)
    private String fcmToken;
    
    public Member updateMember(MemberUpdateRequestDto dto, List<MemberTag> newMemberTags) {
        this.nickname = dto.getNickname();
        this.phoneNumber = dto.getPhoneNumber();
        this.gender = dto.getGender();
        this.birthdate = dto.getBirthdate();
        if (this.memberTags == null) {
            this.memberTags = new ArrayList<>();
        } else {
            this.memberTags.clear(); // 기존 값 제거 (orphanRemoval 작동)
        }
        for (MemberTag tag : newMemberTags) {
            tag.setMember(this); // 양방향 연관관계 설정
            this.memberTags.add(tag);
        }
        return this;
    }
    
    // FCM 토큰 업데이트 메서드
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    // FCM 토큰 삭제 메서드
    public void removeFcmToken() {
        this.fcmToken = null;
    }
    
    public void addMemberInfo(MemberAddtionalInfoRequestDto dto, List<MemberTag> newMemberTags) {
        this.phoneNumber = dto.getPhoneNumber();
        this.gender = dto.getGender();
        this.birthdate = dto.getBirthdate();
        
        if (this.memberTags == null) {
            this.memberTags = new ArrayList<>();
        } else {
            this.memberTags.clear(); // 기존 값 제거 (orphanRemoval 작동)
        }
        
        for (MemberTag tag : newMemberTags) {
            tag.setMember(this); // 양방향 연관관계 설정
            this.memberTags.add(tag);
        }
        
    }
}
