package com.study.studycommunity.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberPasswordUpdateRequest(

        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        @Size(
                max = 64,
                message = "현재 비밀번호는 64자를 초과할 수 없습니다."
        )
        String currentPassword,

        @NotBlank(message = "새로운 비밀번호는 필수입니다.")
        @Size(
                min = 8,
                max = 64,
                message = "새로운 비밀번호는 8~64자여야 합니다."
        )
        String newPassword
) {

    // record는 자동으로 toString을 재정의 하기 때문에
    // 비밀번호 노출 방지 위해 다시 재정의
    @Override
    public String toString() {
        return "MemberPasswordUpdateRequest[REDACTED]";
    }
}
