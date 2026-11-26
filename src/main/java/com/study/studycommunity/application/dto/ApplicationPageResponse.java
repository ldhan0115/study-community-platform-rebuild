package com.study.studycommunity.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 신청 목록에 대한 공통 페이징 응답 DTO
public record ApplicationPageResponse<T>(

        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext
) {

    public static <T> ApplicationPageResponse<T> from(
            Page<T> result
    ){

        return new ApplicationPageResponse<>(
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
