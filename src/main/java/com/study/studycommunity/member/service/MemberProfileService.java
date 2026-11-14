package com.study.studycommunity.member.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.dto.CurrentMemberResponse;
import com.study.studycommunity.member.dto.MemberUpdateRequest;
import com.study.studycommunity.member.exception.DuplicateMemberException;
import com.study.studycommunity.member.exception.MemberAccessDeniedException;
import com.study.studycommunity.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;

    // 현재 로그인한 회원 조회
    public CurrentMemberResponse getCurrentMember(String loginId){

        Member member = findAuthenticatedMember(loginId);

        return CurrentMemberResponse.from(member);
    }

    // 회원 정보 수정
    @Transactional
    public CurrentMemberResponse updateProfile(
            String loginId,
            Long memberId,
            MemberUpdateRequest request

    ){
        Member member = findAuthenticatedMember(loginId);

        // 본인 권한 검증
        if(!member.getId().equals(memberId)){
            throw new MemberAccessDeniedException(
                    "본인의 회원 정보만 수정할 수 있습니다."
            );
        }

        // 자신을 제외한 이메일 중복 확인
        if(memberRepository.existsByEmailAndIdNot(
                request.email(),
                member.getId()
        )){
            throw new DuplicateMemberException(
                    "email",
                    "이미 사용 중인 이메일입니다."
            );
        }

        // 자신을 제외한 닉네임 중복 확인
        if(memberRepository.existsByNicknameAndIdNot(
                request.nickname(),
                member.getId()
        )){
            throw new DuplicateMemberException(
                    "nickname",
                    "이미 사용 중인 닉네임입니다."
            );
        }

        // 영속 상태의 Entity 변경
        member.updateProfile(
                request.email(),
                request.nickname()
        );


        // 변경 감지를 통한 UPDATE 및 DB 제약조건 확인
        try{
            memberRepository.flush();
        }catch(DataIntegrityViolationException exception){

            if(!isDuplicateKey(exception)){
                throw exception;
            }

            throw new DuplicateMemberException(
                    "member",
                    "이미 사용 중인 회원 정보가 존재합니다."
            );
        }


        // 수정된 회원 정보 반환
        return CurrentMemberResponse.from(member);
    }

    private Member findAuthenticatedMember(String loginId){

        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );
    }

    // MySQL 중복 키 오류인지 확인
    private boolean isDuplicateKey(Throwable exception){

        Throwable cause = exception;

        while(cause != null){

            if(cause instanceof SQLException sqlException
                    // 1062 -> 중복 키 오류
                    && sqlException.getErrorCode() == 1062
                    && "23000".equals(sqlException.getSQLState())){

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}


