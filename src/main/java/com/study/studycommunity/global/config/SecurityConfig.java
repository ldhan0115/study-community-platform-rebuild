package com.study.studycommunity.global.config;

import com.study.studycommunity.member.security.MemberUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// Spring Security 설정 파일
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MemberUserDetailsService memberUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        // DB 회원 조회와 BCrypt 검증을 연결
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(memberUserDetailsService);

        authenticationProvider.setPasswordEncoder(passwordEncoder);

        http
                .authenticationProvider(authenticationProvider)

                .authorizeHttpRequests(
                auth -> auth

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health",
                                "/api/auth/csrf"
                        ).permitAll()

                        // 회원가입은 비로그인 사용자도 가능
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/members"
                        ).permitAll()

                        // 그 외 요청은 인증 필요
                        .anyRequest().authenticated()

           // 로그인 설정
        ).formLogin(form -> form
                        // Spring Security가 로그인 요청을 처리할 URL
                        .loginProcessingUrl("/api/auth/login")
                        // 로그인 ID를 받는 요청 파라미터 이름
                        .usernameParameter("loginId")
                        //  로그인 성공: 204 No Content
                        .successHandler(
                                (request, response, authentication) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_NO_CONTENT
                                    );
                                }
                                )

                                .failureHandler(
                                        ((request, response, exception) -> {
                                            response.setStatus(
                                                    HttpServletResponse.SC_UNAUTHORIZED
                                            );
                                        }
                                )
                        ).permitAll()
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        // Spring Security가 로그아웃 요청을 처리할 URL
                        .logoutUrl("/api/auth/logout")
                        // 로그아웃할 때 세션 무효화
                        .invalidateHttpSession(true)
                        // 현재 인증 정보 제거
                        .clearAuthentication(true)

                        // 로그아웃 성공: 204 No Content
                        .logoutSuccessHandler(
                                (request, response, authentication) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_NO_CONTENT
                                    );
                                }
                                ).permitAll()
                        )
                //  인증되지 않은 사용자의 접근은 401 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                );

        // CSRF 보호는 기본값인 활성 상태로 유지
        return http.build();
    }
}
