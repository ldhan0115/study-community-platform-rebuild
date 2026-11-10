package com.study.studycommunity.member.dto;

import com.study.studycommunity.member.domain.Member;

// 응답 전용 DTO를 사용해서 외부에 공개할 정보만 선택적으로 전달
public record MemberSignupResponse(
        Long memberId,
        String loginId,
        String email,
        String nickname
) {

    public static MemberSignupResponse from(Member member){
        return new MemberSignupResponse(
                member.getId(),
                member.getLoginId(),
                member.getEmail(),
                member.getNickname()
        );
    }
}
