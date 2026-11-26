package com.study.studycommunity.application.controller;

import com.study.studycommunity.application.dto.ApplicationCreateRequest;
import com.study.studycommunity.application.dto.ApplicationPageResponse;
import com.study.studycommunity.application.dto.ApplicationResponse;
import com.study.studycommunity.application.dto.StudyApplicationItemResponse;
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

    // 개설자의 참가 목록 조회
    @GetMapping
    public ResponseEntity
            <ApplicationPageResponse
                    <StudyApplicationItemResponse>> getStudyApplications(
                            @PathVariable("studyId") Long studyId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "latest") String sort,
                            Authentication authentication
    ){

        ApplicationPageResponse<StudyApplicationItemResponse> response = applicationService.getStudyApplications(
                authentication.getName(),
                studyId,
                page,
                size,
                sort
        );

        return ResponseEntity.ok(response);
    }


}
