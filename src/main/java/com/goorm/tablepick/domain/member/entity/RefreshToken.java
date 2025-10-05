package com.goorm.tablepick.domain.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @OneToOne
    @JoinColumn(name="member_id", unique = true)
    private Member member;

    @Column(length = 255, nullable = false)
    private String token;

    @UpdateTimestamp

    private LocalDateTime updatedAt;
    private LocalDateTime expiredAt;

    @Builder
    public RefreshToken(Member member, String token, LocalDateTime updatedAt,
                        LocalDateTime expiredAt) {
        this.member = member;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public void updateToken(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }
}
