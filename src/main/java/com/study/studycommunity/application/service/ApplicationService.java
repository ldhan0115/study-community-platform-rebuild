package com.study.studycommunity.application.service;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.dto.ApplicationCreateRequest;
import com.study.studycommunity.application.dto.ApplicationResponse;
import com.study.studycommunity.application.exception.DuplicateApplicationException;
import com.study.studycommunity.application.exception.OwnStudyApplicationException;
import com.study.studycommunity.application.exception.StudyNotOpenException;
import com.study.studycommunity.application.repository.ApplicationRepository;
import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.exception.StudyNotFoundException;
import com.study.studycommunity.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

}
