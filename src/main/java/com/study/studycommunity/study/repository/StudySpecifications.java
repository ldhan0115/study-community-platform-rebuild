package com.study.studycommunity.study.repository;

import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class StudySpecifications {

    private StudySpecifications(){

    }

    // 스터디 조건 검색
    public static Specification<Study> search(

            String keyword,
            StudyMethod method,
            StudyStatus status,
            String region
    ){

        // 논리 삭제된 스터디는 항상 제외
        Specification<Study> spec = (root, query, cb) ->
                cb.isNull(root.get("deletedAt"));

        // 제목 키워드 검색
        if(hasText(keyword)){

            String pattern = containsPattern(keyword);

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("title")),
                            pattern,
                            '!'
                    )
            );
        }

        // 진행 방식 검색
        if(method != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("method"), method)
            );
        }

        // 모집 상태 검색
        if(status != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        // 지역 부분 일치 검색
        if(hasText(region)){
            String pattern = containsPattern(region);

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("region")),
                            pattern,
                            '!'
                    )
            );
        }

        return spec;
    }

    private static boolean hasText(String value){
        return value != null && !value.isBlank();
    }

    // %, _, ! 이스케이프 처리
    private static String containsPattern(String value){

        String escaped = value.strip()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}
