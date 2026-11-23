package com.study.studycommunity.study.controller;

import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.dto.StudyCreateRequest;
import com.study.studycommunity.study.dto.StudyPageResponse;
import com.study.studycommunity.study.dto.StudyResponse;
import com.study.studycommunity.study.dto.StudyUpdateRequest;
import com.study.studycommunity.study.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    // 스터디 개설
    @PostMapping
    public ResponseEntity<StudyResponse> create(
            @Valid @RequestBody StudyCreateRequest request,
            Authentication authentication
            ){

        StudyResponse response = studyService.create(
                authentication.getName(),
                request
        );

        URI location = URI.create(
                "/api/studies/" + response.studyId()
        );

        return ResponseEntity
                .created(location) // 201반환
                .body(response);
    }

    // 스터디 상세 조회
    @GetMapping("/{studyId}")
    public ResponseEntity<StudyResponse> getDetail(
            @PathVariable("studyId") Long studyId
    ){

        StudyResponse response = studyService.getDetail(studyId);

        return ResponseEntity.ok(response);
    }

    // 스터디 목록 조회
    @GetMapping
    public ResponseEntity<StudyPageResponse> getList(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            StudyMethod method,

            @RequestParam(required = false)
            StudyStatus status,

            @RequestParam(required = false)
            String region,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "latest")
            String sort
    ){

        StudyPageResponse response = studyService.getList(
                keyword,
                method,
                status,
                region,
                page,
                size,
                sort
        );

        return ResponseEntity.ok(response);
    }

    // 스터디 수정
    @PutMapping("/{studyId}")
    public ResponseEntity<StudyResponse> update(
            @PathVariable("studyId") Long studyId,
            @Valid @RequestBody StudyUpdateRequest request,
            Authentication authentication
    ){

        StudyResponse response = studyService.update(
                authentication.getName(),
                studyId,
                request
        );

        return ResponseEntity.ok(response);
    }
}
