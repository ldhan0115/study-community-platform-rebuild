package com.study.studycommunity.study.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.dto.StudyCreateRequest;
import com.study.studycommunity.study.dto.StudyPageResponse;
import com.study.studycommunity.study.dto.StudyResponse;
import com.study.studycommunity.study.dto.StudySummaryResponse;
import com.study.studycommunity.study.exception.InvalidStudySearchException;
import com.study.studycommunity.study.exception.StudyNotFoundException;
import com.study.studycommunity.study.repository.StudyRepository;
import com.study.studycommunity.study.repository.StudySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    // 스터디 상세 조회
    public StudyResponse getDetail(Long studyId){

        Study study = studyRepository.findDetailById(studyId)
                .orElseThrow(StudyNotFoundException::new);

        return StudyResponse.from(study);
    }

    // 스터디 목록 조회
    public StudyPageResponse getList(
            String keyword,
            StudyMethod method,
            StudyStatus status,
            String region,
            int page,
            int size,
            String sort
    ){

        // 검색 및 페이징 요청 값 검증
        validateSearchRequest(keyword, region, page, size, sort);

        // 정렬 조건 생성
        Sort sorting = createSort(sort);

        // 페이징 조건 생성
        PageRequest pageable = PageRequest.of(
                page,
                size,
                sorting
        );

        // 동적 검색 조건 생성
        Specification<Study> specification =
                StudySpecifications.search(
                        keyword, method, status, region
                );


        // DB에서 검색 및 페이징
        Page<Study> studyPage = studyRepository.findAll(
                specification,
                pageable
        );

        // 목록 전용 DTO로 변환
        Page<StudySummaryResponse> responsePage =
                studyPage.map(StudySummaryResponse::from);

        // 페이징 메타데이터와 함께 반환
        return StudyPageResponse.from(responsePage);
    }

    // 페이징 및 검색 조건 검증
    private void validateSearchRequest(
        String keyword,
        String region,
        int page,
        int size,
        String sort
    ){

        if(page < 0){
            throw new InvalidStudySearchException(
                    "page",
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if(size < 1 || size > 50){
            throw new InvalidStudySearchException(
                    "size",
                    "페이지 크기는 1~50이어야 합니다."
            );
        }

        if(keyword != null && keyword.strip().length() > 100){
            throw new InvalidStudySearchException(
                    "keyword",
                    "검색어는 100자를 초과할 수 없습니다."
            );
        }

        if(region != null && region.strip().length() > 100){
            throw new InvalidStudySearchException(
                    "region",
                    "지역 검색어는 100자를 초과할 수 없습니다."
            );
        }

        if(!"latest".equals(sort) && !"oldest".equals(sort)){
            throw new InvalidStudySearchException(
                    "sort",
                    "정렬 방식은 latest 또는 oldest여야 합니다."
            );
        }
    }

    // 정렬 조건 설정
    private Sort createSort(String sort){

        // 오래된 순
        if("oldest".equals(sort)){
            return Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
        }

        // 최신 순
        return Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
    }
}
