package com.study.studycommunity.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// record : 데이터를 전달하는 객체를 간결하게 작성할 수 있음
// -> 일반 클래스에서 직접 작성해야 하는 생성자, 필드 접근자 등을 자동으로 제공
public record MemberSignupRequest(

        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4~50자여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
        String password,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
        String nickname
) {
}
