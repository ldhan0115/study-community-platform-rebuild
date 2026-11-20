package com.study.studycommunity.study.controller;

import com.study.studycommunity.study.dto.StudyCreateRequest;
import com.study.studycommunity.study.dto.StudyResponse;
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
}
