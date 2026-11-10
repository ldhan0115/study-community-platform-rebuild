package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.dto.MemberSignupRequest;
import com.study.studycommunity.member.dto.MemberSignupResponse;
import com.study.studycommunity.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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
}
