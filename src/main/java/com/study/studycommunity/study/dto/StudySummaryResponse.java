package com.study.studycommunity.study.dto;

import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;

import java.time.LocalDateTime;

// 스터디 목록 DTO
public record StudySummaryResponse(

        Long studyId,
        Long memberId,
        String ownerNickname,
        String title,
        StudyMethod method,
        String region,
        int capacity,
        StudyStatus status,
        LocalDateTime createdAt
) {

    public static StudySummaryResponse from(Study study){

        return new StudySummaryResponse(

                study.getId(),
                study.getMember().getId(),
                study.getMember().getNickname(),
                study.getTitle(),
                study.getMethod(),
                study.getRegion(),
                study.getCapacity(),
                study.getStatus(),
                study.getCreatedAt()
        );
    }

}
