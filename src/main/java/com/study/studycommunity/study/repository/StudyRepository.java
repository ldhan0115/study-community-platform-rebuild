package com.study.studycommunity.study.repository;

import com.study.studycommunity.study.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    // 삭제되지 않은 스터디 조회
    Optional<Study> findByIdAndDeletedAtIsNull(Long id);

    // 특정 회원이 개설한 스터디 조회
    List<Study> findByMember_IdAndDeletedAtIsNull(Long memberId);
}
