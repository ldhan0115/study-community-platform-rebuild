package com.study.studycommunity.application.repository;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 동일 회원이 해당 스터디에 신청했는지 확인
    boolean existsByStudy_IdAndMember_Id(
            Long studyId,
            Long memerId
    );

    // 특정 회원의 특정 스터디 신청 조회
    Optional<Application> findByStudy_IdAndMember_Id(
            Long studyId,
            Long memerId
    );

    // 특정 스터디에 들어온 신청 목록 조회
    List<Application> findByStudy_IdOrderByCreatedAtDescIdDesc(
            Long studyId
    );

    // 특정 회원의 신청 목록 조회
    List<Application> findByMember_IdOrderByCreatedAtDescIdDesc(
            Long memberId
    );

    // 특정 스터디의 상태별 신청 수
    long countByStudy_IdAndStatus(
            Long studyId,
            ApplicationStatus status
    );

    // 스터디 개설자용 신청 목록 조회 -> 회원 정보 함께 조회
    @EntityGraph(attributePaths = {"member"})
    @Query(
            value = """
                    SELECT a
                    FROM Application a
                    WHERE a.study.id = :studyId
                    """,
            countQuery = """
                         SELECT COUNT(a)
                         FROM Application a
                         WHERE a.study.id = :studyId
                         """
    )
    Page<Application> findPageByStudyId(
            @Param("studyId") Long studyId,
            Pageable pageable
    );

    // 신청자용 신청 목록 조회 -> 신청 대상 스터디 정보 함께 조회
    @EntityGraph(attributePaths = {"study"})
    @Query(
            value = """
                SELECT a
                FROM Application a
                WHERE a.member.id = :memberId
                """,
            countQuery = """
                SELECT COUNT(a)
                FROM Application a
                WHERE a.member.id = :memberId
                """
    )
    Page<Application> findPageByMemberId(
            @Param("memberId") Long memberId,
            Pageable pageable
    );


}
