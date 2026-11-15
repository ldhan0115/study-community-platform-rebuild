package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.dto.MemberPasswordUpdateRequest;
import com.study.studycommunity.member.service.MemberPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberPasswordController {

    private final MemberPasswordService memberPasswordService;

    private final SecurityContextLogoutHandler logoutHandler
            = new SecurityContextLogoutHandler();

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody MemberPasswordUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ){


        //  인증된 회원 비밀번호 변경
        memberPasswordService.changePassword(
                authentication.getName(),
                request
        );

        // 비밀번호 변경 성공 이후 현재 세션 무효화
        logoutHandler.logout(
                httpRequest,
                httpResponse,
                authentication
        );

        // 성공 응답
        return ResponseEntity.noContent().build();
    }
}
