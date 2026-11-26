package com.study.studycommunity.application.controller;

import com.study.studycommunity.application.dto.ApplicationPageResponse;
import com.study.studycommunity.application.dto.MyApplicationItemResponse;
import com.study.studycommunity.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final ApplicationService applicationService;

    // 내 참가 신청 목록 조회
    @GetMapping("/me")
    public ResponseEntity<
            ApplicationPageResponse<
                    MyApplicationItemResponse>> getMyApplications(
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "latest") String sort,
                            Authentication authentication
    ){

        ApplicationPageResponse<MyApplicationItemResponse> response = applicationService.getMyApplications(
                authentication.getName(),
                page,
                size,
                sort
        );

        return ResponseEntity.ok(response);
    }

}
