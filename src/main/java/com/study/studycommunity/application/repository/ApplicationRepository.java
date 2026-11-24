package com.study.studycommunity.application.repository;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
