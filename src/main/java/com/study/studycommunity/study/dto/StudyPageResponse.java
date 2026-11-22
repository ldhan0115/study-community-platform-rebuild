package com.study.studycommunity.study.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 페이징 결과 DTO
public record StudyPageResponse(

        List<StudySummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext
) {

    public static StudyPageResponse from(
            Page<StudySummaryResponse> result
    ){

        return new StudyPageResponse(

                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast(),
                result.hasNext()
        );

    }
}
