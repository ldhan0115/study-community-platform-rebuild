package com.study.studycommunity.study.dto;

import com.study.studycommunity.study.domain.StudyMethod;
import jakarta.validation.constraints.*;

public record StudyCreateRequest(

        @NotBlank(message = "스터디 제목은 필수입니다.")
        @Size(
                max = 100,
                message = "스터디 제목은 100자를 초과할 수 없습니다."
        )
        String title,

        @NotBlank(message = "스터디 내용은 필수입니다.")
        @Size(
                max = 10000,
                message = "스터디 내용은 10000자를 초과할 수 없습니다."
        )
        String description,

        @NotNull(message = "스터디 진행 방식은 필수입니다.")
        StudyMethod method,

        @Size(
                max = 100,
                message = "지역은 100자를 초과할 수 없습니다."
        )
        String region,

        @NotNull(message = "모집 인원은 필수입니다.")
        @Min(
                value = 1,
                message = "모집 인원은 최소 1명이어야 합니다."
        )
        @Max(
                value = 30,
                message = "모집 인원은 최대 30명이어야 합니다."
        )
        Integer capacity

) {
}
