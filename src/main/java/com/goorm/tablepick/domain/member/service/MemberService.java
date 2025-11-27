package com.goorm.tablepick.domain.member.service;

import com.goorm.tablepick.domain.member.entity.Member;
import com.goorm.tablepick.domain.member.repository.MemberRepository;
import com.goorm.tablepick.domain.reservation.dto.response.ReservationResponseDto;
import com.goorm.tablepick.domain.reservation.entity.Reservation;
import com.goorm.tablepick.domain.reservation.repository.ReservationRepository;
import com.goorm.tablepick.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService{
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final TagRepository tagRepository;

//    @Override
//    public MemberResponseDto getMemberInfo(String username) {
//        Member member = memberRepository.findByEmail(username)
//                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
//        return MemberResponseDto.toDto(member);
//    }

//    @Transactional
//    public void updateMemberInfo(String username, MemberUpdateRequestDto memberUpdateRequestDto) {
//        Member member = memberRepository.findByEmail(username)
//                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
//        List<Tag> selectedTags = new ArrayList<>();
//        for (Long id : memberUpdateRequestDto.getMemberTags()) {
//            Tag tag = tagRepository.findById(id).orElseThrow(() -> new MemberException(MemberErrorCode.NO_SUCH_TAG));
//            selectedTags.add(tag);
//        }
//
//        List<MemberTag> newMemberTags = selectedTags.stream().map(tag -> MemberTag.create(member, tag))
//                .toList();
//
//        Member updatedMember = member.updateMember(memberUpdateRequestDto, newMemberTags);
//        memberRepository.save(updatedMember);
//    }


    public List<ReservationResponseDto> getMemberReservationList(String username) {
        List<Reservation> reservationList = reservationRepository.findAllByMemberEmail(username);

        return reservationList.stream()
                .map(ReservationResponseDto::toDto)
                .collect(Collectors.toList());
    }


    public Member getMember(String email) {
        return memberRepository.findByEmail(email).get();
    }

//    @Transactional
//    public void addMemberInfo(String username, MemberAddtionalInfoRequestDto dto) {
//        Member member = memberRepository.findByEmail(username)
//                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
//
//        List<MemberTag> memberTagList = dto.getMemberTags().stream()
//                .map(tagId -> {
//                    Tag tag = tagRepository.findById(tagId)
//                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다: " + tagId));
//                    return new MemberTag(member, tag);
//                })
//                .collect(Collectors.toList());
//
//        member.addMemberInfo(dto, memberTagList);
//
//        memberTagRepository.saveAll(memberTagList);
//        // memberRepository.save(member); → 변경 감지로 생략 가능
//
//        // 회원가입 축하 알림은 이미 OAuth2 로그인 시 전송되었으므로 여기서는 전송하지 않음
//        log.info("회원 추가 정보 입력 완료: {}, ID: {}", member.getEmail(), member.getId());
//    }

}
