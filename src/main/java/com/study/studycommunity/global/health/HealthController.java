package com.study.studycommunity.global.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 서버가 정상적으로 실행 중인지 확인하기 위한 API
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health(){
        return Map.of("status", "UP");
    }
}
