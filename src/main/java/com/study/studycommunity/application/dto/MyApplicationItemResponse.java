package com.study.studycommunity.application.dto;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;

import java.time.LocalDateTime;

// 내 신청 목록용 DTO
public record MyApplicationItemResponse(

        Long applicationId,
        Long studyId,
        String studyTitle,
        boolean studyDeleted,
        String message,
        ApplicationStatus status,
        LocalDateTime createdAt
) {

    public static MyApplicationItemResponse from(
            Application application
    ) {

        return new MyApplicationItemResponse(
                application.getId(),
                application.getStudy().getId(),
                application.getStudy().getTitle(),
                application.getStudy().getDeletedAt() != null,
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
