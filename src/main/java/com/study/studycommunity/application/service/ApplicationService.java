package com.study.studycommunity.application.service;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.dto.*;
import com.study.studycommunity.application.exception.DuplicateApplicationException;
import com.study.studycommunity.application.exception.InvalidApplicationQueryException;
import com.study.studycommunity.application.exception.OwnStudyApplicationException;
import com.study.studycommunity.application.exception.StudyNotOpenException;
import com.study.studycommunity.application.repository.ApplicationRepository;
import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.exception.StudyAccessDeniedException;
import com.study.studycommunity.study.exception.StudyNotFoundException;
import com.study.studycommunity.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    private final ApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;

    // 스터디 참가 신청
    @Transactional
    public ApplicationResponse create(
            String loginId,
            Long studyId,
            ApplicationCreateRequest request
    ){

        // 신청 대상 스터디 조회
        Study study = studyRepository.findDetailById(studyId)
                .orElseThrow(StudyNotFoundException::new);

        // 현재 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // 자신의 스터디에 신청 차단
        if(study.getMember().getId().equals(member.getId())){
            throw new OwnStudyApplicationException();
        }

        // 모집 마감인 경우 신청 차단
        if(study.getStatus() != StudyStatus.OPEN){
            throw new StudyNotOpenException();
        }

        // 중복 신청 사전 검사
        boolean alreadyApplied = applicationRepository.existsByStudy_IdAndMember_Id(
                studyId,
                member.getId()
        );

        if(alreadyApplied){
            throw new DuplicateApplicationException();
        }

        // 참가 신청 entity 생성
        Application application = Application.create(
                member,
                study,
                request.message()
        );

        // DB 저장
        try{
            Application saved = applicationRepository.saveAndFlush(application);

            return ApplicationResponse.from(saved);
        }catch (DataIntegrityViolationException exception){

            // 동시 요청으로 DB UNIQUE 충돌이 발생한 경우
            if(isDuplicateKeyViolation(exception)){
                throw new DuplicateApplicationException();
            }

            // 다른 DB 제약조건 오류는 숨기지 않는다
            throw exception;
        }

    }

    // 개설자의 참가 신청 목록 조회
    public ApplicationPageResponse<StudyApplicationItemResponse> getStudyApplications(
            String loginId,
            Long studyId,
            int page,
            int size,
            String sort
    ){

        // 삭제되지 않은 스터디 조회
        Study study = studyRepository.findDetailById(studyId)
                .orElseThrow(StudyNotFoundException::new);

        // 현재 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // 스터디 개설자 권한 확인
        if(!study.getMember().getId().equals(member.getId())){
            throw new StudyAccessDeniedException(
                    "해당 스터디의 참가 신청 목록을 조회할 권한이 없습니다."
            );
        }

        // 페이징 생성
        Pageable pageable = createPageable(
                page,
                size,
                sort
        );

        // 신청 목록 조회
        Page<Application> applications = applicationRepository.findPageByStudyId(
                studyId,
                pageable
        );

        // Entity -> DTO 변환
        Page<StudyApplicationItemResponse> responsePage =
                applications.map(StudyApplicationItemResponse::from);

        return ApplicationPageResponse.from(responsePage);
    }

    // 내 참가 신청 목록 조회
    public ApplicationPageResponse<MyApplicationItemResponse> getMyApplications(
            String loginId,
            int page,
            int size,
            String sort
    ){

        // 현재 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // 페이징 생성
        Pageable pageable = createPageable(
                page,
                size,
                sort
        );

        // 로그인한 회원의 신청만 조회
        Page<Application> applications = applicationRepository.findPageByMemberId(
                member.getId(),
                pageable
        );

        // DTO 변환
        Page<MyApplicationItemResponse> responsePage = applications.map(MyApplicationItemResponse::from);

        return ApplicationPageResponse.from(responsePage);
    }

    private boolean isDuplicateKeyViolation(Throwable exception) {

        Throwable cause = exception;

        while (cause != null) {

            if(cause instanceof SQLException sqlException
                && sqlException.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR){
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    // Pageable 생성 공통 메서드
    private Pageable createPageable(
            int page,
            int size,
            String sort
    ){

        if(page < 0){
            throw new InvalidApplicationQueryException(
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if(size < 1 || size > 50){
            throw new InvalidApplicationQueryException(
                    "페이지 크기는 1~50이어야 합니다."
            );
        }

        Sort.Direction direction;

        if("latest".equals(sort)){
            direction = Sort.Direction.DESC;
        }else if("oldest".equals(sort)){
            direction = Sort.Direction.ASC;
        }else{
            throw new InvalidApplicationQueryException(
                    "정렬 기준은 latest 또는 oldest여야 합니다."
            );
        }

        Sort sorting = Sort.by(
                direction,
                "createdAt",
                "id"
        );

        return PageRequest.of(
                page,
                size,
                sorting
        );
    }

}
