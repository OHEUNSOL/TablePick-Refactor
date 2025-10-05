package com.goorm.tablepick.domain.member.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.goorm.tablepick.domain.member.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "로그인 후 사용자 정보 추가")
public class MemberAddtionalInfoRequestDto {

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "생년월일", example = "2002-01-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    @Schema(description = "전화번호", example = "01098765432")
    @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
    private String phoneNumber;

    @Schema(description = "사용자 선호 태그")
    @Max(value = 5, message = "선호 태그는 5개까지만 가능합니다.")
    @Min(value = 1, message = "선호 태그는 1개 이상 선택해주세요.")
    private List<Long> memberTags;

}
