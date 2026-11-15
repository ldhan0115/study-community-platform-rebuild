package com.study.studycommunity.member.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.dto.MemberPasswordUpdateRequest;
import com.study.studycommunity.member.exception.InvalidMemberPasswordException;
import com.study.studycommunity.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPasswordService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 변경
    @Transactional
    public void changePassword(
            String loginId,
            MemberPasswordUpdateRequest request
    ){

        // 현재 인증된 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // BCrypt 입력 바이트 길이 검증
        validatePasswordBytes(
                "currentPassword",
                request.currentPassword()
        );

        validatePasswordBytes(
                "newPassword",
                request.newPassword()
        );

        // 현재 비밀번호 일치 여부 확인
        if(!passwordEncoder.matches(
                request.currentPassword(),
                member.getPasswordHash()
        )){
            throw new InvalidMemberPasswordException(
                    "currentPassword",
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }

        // 기존 비밀번호와 동일한지 확인
        if(passwordEncoder.matches(
                request.newPassword(),
                member.getPasswordHash()
        )){
            throw new InvalidMemberPasswordException(
                    "newPassword",
                    "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다"
            );
        }

        // 새로운 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.newPassword());

        // entity 상태 변경
        member.changePassword(encodedPassword);


    }

    private void validatePasswordBytes(
            String field,
            String password
    ){

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;

        if(passwordBytes > 72){
            throw new InvalidMemberPasswordException(
                    field,
                    "비밀번호는 UTF-8 기준 72바이트를 초과할 수 없습니다."
            );
        }
    }

}
