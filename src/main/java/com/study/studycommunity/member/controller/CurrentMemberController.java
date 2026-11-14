package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.dto.CurrentMemberResponse;
import com.study.studycommunity.member.service.MemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CurrentMemberController {

    private final MemberProfileService memberProfileService;

    @GetMapping("/me")
    public CurrentMemberResponse me(Authentication authentication){

        // 기존 repository에 의존하던 것에서 조회 로직을 service 계층에 두는 것으로 변경
        return memberProfileService.getCurrentMember(
                authentication.getName()
        );

    }
}
