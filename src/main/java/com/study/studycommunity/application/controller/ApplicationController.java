package com.study.studycommunity.application.controller;

import com.study.studycommunity.application.dto.ApplicationCreateRequest;
import com.study.studycommunity.application.dto.ApplicationResponse;
import com.study.studycommunity.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/studies/{studyId}/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // 스터디 참가 신청
    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @PathVariable("studyId") Long studyId,
            @Valid @RequestBody ApplicationCreateRequest request,
            Authentication authentication
    ){

        ApplicationResponse response = applicationService.create(
                authentication.getName(),
                studyId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


}
