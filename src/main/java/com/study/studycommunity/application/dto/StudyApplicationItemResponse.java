package com.study.studycommunity.application.dto;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;

import java.time.LocalDateTime;

// 스터디 개설자용 신청자 목록 DTO
public record StudyApplicationItemResponse(

        Long applicationId,
        Long memberId,
        String applicantNickname,
        String message,
        ApplicationStatus status,
        LocalDateTime createdAt

) {

    public static StudyApplicationItemResponse from(
            Application application
    ) {

        return new StudyApplicationItemResponse(
                application.getId(),
                application.getMember().getId(),
                application.getMember().getNickname(),
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}