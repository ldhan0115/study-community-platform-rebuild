package com.study.studycommunity.application.dto;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;

import java.time.LocalDateTime;

// 스터디 신청 응답 DTO
public record ApplicationResponse(

        Long applicationId,
        Long studyId,
        Long memberId,
        String message,
        ApplicationStatus status,
        LocalDateTime createdAt
) {

    public static ApplicationResponse from(
            Application application
    ){

        return new ApplicationResponse(
                application.getId(),
                application.getStudy().getId(),
                application.getMember().getId(),
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
