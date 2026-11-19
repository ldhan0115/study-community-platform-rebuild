package com.study.studycommunity.study.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.dto.StudyCreateRequest;
import com.study.studycommunity.study.dto.StudyResponse;
import com.study.studycommunity.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;

    // 스터디 개설
    @Transactional
    public StudyResponse create(
            String loginId,
            StudyCreateRequest request
    ){

        // 현재 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // 스터디 생성 및 도메인 규칙 검증
        Study study = Study.create(
                member,
                request.title(),
                request.description(),
                request.method(),
                request.region(),
                request.capacity()
        );

        // 데이터베이스 저장
        Study savedStudy = studyRepository.saveAndFlush(study);

        // 응답 DTO 변환
        return StudyResponse.from(savedStudy);

    }
}
