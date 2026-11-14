package com.study.studycommunity.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(
                max = 254,
                message = "이메일은 254자를 초과할 수 없습니다."
        )
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                min = 2,
                max = 30,
                message = "닉네임은 2~30자여야 합니다."
        )
        String nickname

) {
}
