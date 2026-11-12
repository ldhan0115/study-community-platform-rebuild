package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.dto.CurrentMemberResponse;
import com.study.studycommunity.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CurrentMemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/me")
    public CurrentMemberResponse me(Authentication authentication){

        // 현재 로그인한 사용자의 loginId를 가져옴 -> UserDetailService에서 설정한 값
        String loginId = authentication.getName();

        // 실제 DB에서 최신 회원 정보 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "인증된 회원을 찾을 수 없습니다."
                        )
                );

        // 비밀번호 해시를 제외한 응답 DTO 반환
        return CurrentMemberResponse.from(member);
    }
}
