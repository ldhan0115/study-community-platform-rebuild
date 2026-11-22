package com.study.studycommunity.study.repository;

import com.study.studycommunity.study.domain.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyRepository
        extends JpaRepository<Study, Long>,
                JpaSpecificationExecutor<Study> {

    // 삭제되지 않은 스터디 조회
    Optional<Study> findByIdAndDeletedAtIsNull(Long id);

    // 특정 회원이 개설한 스터디 조회
    List<Study> findByMember_IdAndDeletedAtIsNull(Long memberId);

    // 스터디 상세 조회 (fetch 조인을 통해 연관 객체 member와 함께 조회)
    @Query("""
            SELECT s
            FROM Study s
            JOIN FETCH s.member
            WHERE s.id = :studyId
            AND s.deletedAt Is NULL
            """)
    Optional<Study> findDetailById(
            @Param("studyId") Long studyId
    );


    // Specification의 검색 조건을 기반으로 스터디 목록 조회
    // Pageable에 설정한 페이지 번호와 크기 적용
    // @EntityGraph를 적용해 개설자 정보를 함께 로딩하도록 설정
    @Override
    @EntityGraph(attributePaths = "member")
    Page<Study> findAll(Specification<Study> spec, Pageable pageable);
}
