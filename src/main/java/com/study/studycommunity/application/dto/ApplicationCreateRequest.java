package com.study.studycommunity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 스터디 신청 DTO
public record ApplicationCreateRequest(

        @NotBlank(message = "신청 메시지는 필수입니다.")
        @Size(
                max = 255,
                message = "신청 메시지는 255자를 초과할 수 없습니다."
        )
        String message
) {
}
