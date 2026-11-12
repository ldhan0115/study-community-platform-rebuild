package com.study.studycommunity.member.dto;

import com.study.studycommunity.member.domain.Member;

// 로그인한 회원 정보 반환 객체
public record CurrentMemberResponse(
        Long memberId,
        String loginId,
        String email,
        String nickname
) {

    public static CurrentMemberResponse from(Member member){
        return new CurrentMemberResponse(
                member.getId(),
                member.getLoginId(),
                member.getEmail(),
                member.getNickname()
        );
    }
}
