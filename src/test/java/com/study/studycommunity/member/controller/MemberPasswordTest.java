package com.study.studycommunity.member.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MemberPasswordTest {

    private static final String OLD_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member member;

    @BeforeEach
    void setUp(){

        String loginId = "pw_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

       member = memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode(OLD_PASSWORD),
                        loginId + "@test.com",
                        loginId
                )
        );
    }


    @AfterEach
    void tearDown(){

        if(member != null){
            memberRepository.deleteById(member.getId());
        }
    }

    // 비밀번호 변경 성공 테스트
    @Test
    void changePasswordSuccessfully() throws Exception {
        // given
        MockHttpSession session = login(OLD_PASSWORD);

        //when & then
        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // 실제 DB에서 변경된 회원 조회
        Member updated = memberRepository.findById(member.getId())
                .orElseThrow();

        // 기존 해시와 달라야함
        assertThat(updated.getPasswordHash())
                .isNotEqualTo(member.getPasswordHash());

        // 평문으로 저장 x
        assertThat(updated.getPasswordHash())
                .isNotEqualTo(NEW_PASSWORD);

        // 새로운 비밀번호와 일치
        assertThat(passwordEncoder.matches(
                NEW_PASSWORD,
                updated.getPasswordHash()
        )).isTrue();

        // 기존 비밀번호와 일치 x
        assertThat(passwordEncoder.matches(
                OLD_PASSWORD,
                updated.getPasswordHash()
        )).isFalse();

        // 비밀번호 이외의 정보는 유지
        assertThat(updated.getLoginId())
                .isEqualTo(member.getLoginId());

        assertThat(updated.getEmail())
                .isEqualTo(member.getEmail());
    }

    // 비밀번호 변경 후 기존 비밀번호 사용 불가 테스트
    @Test
    void oldPasswordCannotBeUsedAfterChange() throws Exception {
        // given
        changePassword();

        //when & then
        mockMvc.perform(loginRequest(OLD_PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    // 비밀번호 변경 후 새로운 비밀번호로 로그인 성공 테스트
    @Test
    void newPasswordCanBeUsedAfterChange() throws Exception {

        // given
        changePassword();

        //when
        MockHttpSession newSession = login(NEW_PASSWORD);

        //then
        mockMvc.perform(
                get("/api/auth/me")
                        .session(newSession)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value(member.getLoginId()));
    }

    // 비밀번호 변경 후 기존 세션 비활성화 테스트
    @Test
    void sessionIsInvalidatedAfterPasswordChange() throws Exception {

        // given
        MockHttpSession session = login(OLD_PASSWORD);

        //when
        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        //then
        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(
                get("/api/auth/me")
        )
                .andExpect(status().isUnauthorized());
    }

    // 비밀번호 변경 시 현재 비밀번호 불일치 하면 변경 실패 테스트
    @Test
    void rejectsIncorrectCurrentPassword() throws Exception {

        // given
        MockHttpSession session = login(OLD_PASSWORD);

        //when & then
        mockMvc.perform(
                changePasswordRequest(
                        "WrongPassword",
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest());
//                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
//                .andExpect(jsonPath("$.field").value("currentPassword"));

        // 실패했으므로 DB의 비밀번호 변경 안된 것 확인
        assertPasswordUnchanged();

        // 기존 세션 유지
        assertThat(session.isInvalid()).isFalse();

        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
                .andExpect(status().isOk());

    }

    // 같은 비밀번호로 변경 시 불가 테스트
    @Test
    void rejectsSamePassword() throws Exception {

        MockHttpSession session = login(OLD_PASSWORD);

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        OLD_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.field").value("newPassword"));

        assertPasswordUnchanged();

        assertThat(session.isInvalid()).isFalse();

    }

    // 비밀번호 최소 길이 미충족시 실패 테스트
    @Test
    void rejectsTooShortPassword() throws Exception {

        MockHttpSession session = login(OLD_PASSWORD);

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        "short"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("newPassword"));

        assertPasswordUnchanged();
    }


    // 비밀번호 길이 UTF-8 기준으로 75초과시 실패 테스트
    @Test
    void rejectsPasswordExceeding72Bytes() throws Exception {

        MockHttpSession session = login(OLD_PASSWORD);

        // 한글 25자 -> UTF-8 기준 75바이트
        String longPassword = "가".repeat(25);

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        longPassword
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.field").value("newPassword"));

        assertPasswordUnchanged();
    }

    // 비로그인 상태로 비밀번호 변경 시도 시 불가 테스트
    @Test
    void cannotChangePasswordWithoutAuthentication() throws Exception {

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        NEW_PASSWORD
                )
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());

        assertPasswordUnchanged();
    }


    // csrf 토큰 없이 비밀번호 변경 시도 시 실패 테스트
    @Test
    void cannotChangePasswordWithoutCsrfToken() throws Exception {

        MockHttpSession session = login(OLD_PASSWORD);

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
        )
                .andExpect(status().isForbidden());

        assertPasswordUnchanged();
        assertThat(session.isInvalid()).isFalse();
    }

    // 테스트용 비밀번호 변경
    private void changePassword() throws Exception {

        MockHttpSession session = login(OLD_PASSWORD);

        mockMvc.perform(
                changePasswordRequest(
                        OLD_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());
    }

    // 실제 로그인 요청 및 세션 반환
    private MockHttpSession login(String password) throws Exception {

        MvcResult result = mockMvc.perform(
                        loginRequest(password)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;


    }

    // 로그인 요청 생성
    private MockHttpServletRequestBuilder loginRequest(
            String password
    ){
        return post("/api/auth/login")
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                )
                .param("loginId", member.getLoginId())
                .param("password", password)
                .with(csrf());
    }

    // 비밀번호 변경 요청 생성
    private MockHttpServletRequestBuilder changePasswordRequest(
            String currentPassword,
            String newPassword
    ){

        String requestBody = """
                {
                    "currentPassword": "%s",
                    "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);

        return put("/api/members/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
    }

    // DB의 비밀번호가 변경되지 않았는지 확인
    private void assertPasswordUnchanged(){

        Member unchanged = memberRepository.findById(member.getId())
                .orElseThrow();

        assertThat(unchanged.getPasswordHash())
                .isEqualTo(member.getPasswordHash());

        assertThat(passwordEncoder.matches(
                OLD_PASSWORD,
                unchanged.getPasswordHash()
        )).isTrue();
    }
}
