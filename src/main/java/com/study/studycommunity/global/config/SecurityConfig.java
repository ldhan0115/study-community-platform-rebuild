package com.study.studycommunity.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// Spring Security 설정 파일
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        http.authorizeHttpRequests(
                auth -> auth.requestMatchers(
                                HttpMethod.GET,
                                "/api/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                // HTML 폼을 사용하는 로그인 방식을 기본 설정으로 활성화
        ).formLogin(Customizer.withDefaults());

        return http.build();
    }
}
