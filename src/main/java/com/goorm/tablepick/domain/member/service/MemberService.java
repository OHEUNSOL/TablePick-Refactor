package com.goorm.tablepick.domain.member.service;

import com.goorm.tablepick.domain.member.dto.MemberAddtionalInfoRequestDto;
import com.goorm.tablepick.domain.member.dto.MemberResponseDto;
import com.goorm.tablepick.domain.member.dto.MemberUpdateRequestDto;
import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.reservation.dto.response.ReservationResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface MemberService {
    MemberResponseDto getMemberInfo(String username);

    void updateMemberInfo(String username, @Valid MemberUpdateRequestDto memberUpdateRequestDto);

    List<ReservationResponseDto> getMemberReservationList(String username);

    void addMemberInfo(String username, @Valid MemberAddtionalInfoRequestDto memberAddtionalInfoRequestDto);

    Member getMember(String userId);
}
