package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.dto.CurrentMemberResponse;
import com.study.studycommunity.member.dto.MemberSignupRequest;
import com.study.studycommunity.member.dto.MemberSignupResponse;
import com.study.studycommunity.member.dto.MemberUpdateRequest;
import com.study.studycommunity.member.service.MemberProfileService;
import com.study.studycommunity.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;


    // 회원가입
    @PostMapping
    public ResponseEntity<MemberSignupResponse> signup(
            // Request DTO에 작성한 Bean Validation 규칙 적용
            // 클라이언트가 전송한 JSON 요청 본문을 Java 객체로 변환
            @Valid @RequestBody MemberSignupRequest request
    ){
        MemberSignupResponse response = memberService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED) // 201 Created
                .body(response);
    }

    // 회원 정보 수정
    @Transactional
    @PutMapping("/{memberId}/profile")
    public ResponseEntity<CurrentMemberResponse> updateProfile(
            @PathVariable("memberId") Long memberId,
            @Valid @RequestBody MemberUpdateRequest request,
            Authentication authentication
    ){

        CurrentMemberResponse response = memberProfileService.updateProfile(
                authentication.getName(),
                memberId,
                request
        );

        return ResponseEntity.ok(response);
    }
}
