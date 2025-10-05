package com.goorm.tablepick.domain.member.entity;

import com.goorm.tablepick.domain.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public MemberTag(Member member, Tag tag) {
        this.member = member;
        this.tag = tag;
    }

    public static MemberTag create(Member member, Tag tag){
        return MemberTag.builder()
                .member(member)
                .tag(tag)
                .build();
    }

}
