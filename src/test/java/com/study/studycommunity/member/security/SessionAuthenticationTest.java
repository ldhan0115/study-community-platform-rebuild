package com.study.studycommunity.member.security;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SessionAuthenticationTest {

    private static final String RAW_PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 테스트용 회원 생성
    private Member createMember(String loginId){

        Member member = Member.create(
                loginId,
                passwordEncoder.encode(RAW_PASSWORD),
                loginId + "@test.com",
                loginId
        );

        return memberRepository.saveAndFlush(member);
    }

    // 실제 로그인 요청 후 세션 반환
    private MockHttpSession login(String loginId) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("loginId", loginId)
                                .param("password", RAW_PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    // 로그인 성공 테스트
    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        // given
        Member member = createMember("member");

        //when
        MockHttpSession session = login(member.getLoginId());

        //then
        // 로그인한 세션으로 회원 정보 조회 가능
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").isNumber())
                .andExpect(jsonPath("$.loginId").value("member"))
                .andExpect(jsonPath("$.email").value("member@test.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // 잘못된 패스워드로 인한 로그인 실패 테스트
    @Test
    void loginFailsWithIncorrectPassword() throws Exception {
        // given
        createMember("member");

        //when & then
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("loginId", "member")
                        .param("password", "wrongPassword")
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());
    }

    // 존재하지 않는 회원 로그인 실패 테스트
    @Test
    void loginFailsWhenMemberDoesNotExist() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("loginId", "unknown")
                        .param("password", RAW_PASSWORD)
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());

    }

    // 로그인 하지 않은 상태로 보호 API 호출 실패 테스트
    @Test
    void currentMemberRequiresAuthentication() throws Exception {

        mockMvc.perform(
                get("/api/auth/me")
        )
                .andExpect(status().isUnauthorized());
    }

    // 로그아웃 성공 테스트
    @Test
    void logoutInvalidatesSession() throws Exception {
        // given
        Member member = createMember("member");
        MockHttpSession session = login(member.getLoginId());

        // 로그인 상태 확인
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
                .andExpect(status().isOk());

        //when
        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        //then
        // 기존 세션 무효화 확인
        assertThat(session.isInvalid()).isTrue();

        // 새 비로그인 요청도 보호 API에 접근할 수 없음
        mockMvc.perform(
                get("/api/auth/me")
        )
                .andExpect(status().isUnauthorized());
    }


    // csrf 토큰 없이 로그인 실패 테스트
    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {

        createMember("member");

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("loginId", "member")
                        .param("password", RAW_PASSWORD)
        )
                .andExpect(status().isForbidden());

    }

    // csrf 토큰 없이 로그아웃 실패 테스트
    @Test
    void logoutWithoutCsrfTokenIsForbidden() throws Exception {

        Member member = createMember("member");
        MockHttpSession session = login(member.getLoginId());

        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
        )
                .andExpect(status().isForbidden());

        // csrf 검증에 실패했으므로 세션이 살아 있어야 함
        assertThat(session.isInvalid()).isFalse();
    }
}
