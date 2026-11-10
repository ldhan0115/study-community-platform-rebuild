package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 회원가입 성공 테스트
    @Test
    void signupSuccess() throws Exception {
        // given
        String requestBody = """
                {
                    "loginId": "member",
                    "password": "password",
                    "email": "member@test.com",
                    "nickname": "멤버" 
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").isNumber())
                .andExpect(jsonPath("$.loginId").value("member"))
                .andExpect(jsonPath("$.email").value("member@test.com"))
                .andExpect(jsonPath("$.nickname").value("멤버"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Member savedMember = memberRepository.findByLoginId("member")
                .orElseThrow();

        assertThat(
                passwordEncoder.matches(
                        "password",
                        savedMember.getPasswordHash()
                )
        ).isTrue();
    }

    // 이메일 입력 형식 미충족 테스트
    @Test
    void signupFailsWithInvalidEmail() throws Exception {
        // given
        String requestBody = """
                {
                    "loginId": "member",
                    "password": "password",
                    "email": "member-test.com",
                    "nickname": "멤버" 
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    // 로그인 아이디 중복으로 인한 회원가입 실패 테스트
    @Test
    void signupFailsWithDuplicateLoginId() throws Exception {
        // given
        memberRepository.saveAndFlush(
                Member.create(
                        "member",
                        passwordEncoder.encode("password"),
                        "member@test.com",
                        "멤버"
                )
        );

        String requestBody = """
                {
                    "loginId": "member",
                    "password": "password",
                    "email": "member@test.com",
                    "nickname": "멤버" 
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_DUPLICATE"))
                .andExpect(jsonPath("$.field").value("loginId"));
    }

    // csrf 토큰 없으면 회원가입 실패 테스트
    @Test
    void signupFailsWithoutCsrfToken() throws Exception {
        // given
        String requestBody = """
                {
                    "loginId": "member",
                    "password": "password",
                    "email": "member@test.com",
                    "nickname": "멤버" 
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isForbidden());
    }

    // 로그인 없이 csrf 토큰 발급 테스트
    @Test
    void csrfTokenIsAccessibleWithoutLogin() throws Exception {

        mockMvc.perform(
                get("/api/auth/csrf")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}