package com.study.studycommunity.study.dto;

import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;

import java.time.LocalDateTime;

public record StudyResponse(

        Long studyId,
        Long memberId,
        String ownerNickname,
        String title,
        String description,
        StudyMethod method,
        String region,
        int capacity,
        StudyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StudyResponse from(Study study){

        return new StudyResponse(
                study.getId(),
                study.getMember().getId(),
                study.getMember().getNickname(),
                study.getTitle(),
                study.getDescription(),
                study.getMethod(),
                study.getRegion(),
                study.getCapacity(),
                study.getStatus(),
                study.getCreatedAt(),
                study.getUpdatedAt()
        );
    }
}
