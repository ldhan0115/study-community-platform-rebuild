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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class MemberProfileTest {

    private static final String RAW_PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member memberA;
    private Member memberB;

    @BeforeEach
    void setup(){

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

        memberA = createMember("a_" + suffix);
        memberB = createMember("b_" + suffix);
    }

    @AfterEach
    void tearDown(){

        // 이번 테스트에서 생성한 회원만 삭제
        if(memberA != null){
            memberRepository.deleteById(memberA.getId());
        }

        if(memberB != null){
            memberRepository.deleteById(memberB.getId());
        }
    }

    // 프로필 수정 성공 테스트
    @Test
    void updateOwnProfile() throws Exception {
        // given
        MockHttpSession session = login(memberA.getLoginId());

        String newEmail = memberA.getLoginId() + "_new@test.com";

        String newNickname = memberA.getLoginId() + "_new";

        //when & then
        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        newEmail,
                        newNickname
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value(memberA.getLoginId()))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.nickname").value(newNickname))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 실제 DB 변경 확인
        Member updated = memberRepository.findById(memberA.getId())
                .orElseThrow();

        assertThat(updated.getEmail()).isEqualTo(newEmail);
        assertThat(updated.getNickname()).isEqualTo(newNickname);

        // 변경하지 않아야 하는 값도 확인
        assertThat(updated.getLoginId())
                .isEqualTo(memberA.getLoginId());

        assertThat(updated.getPasswordHash())
                .isEqualTo(memberA.getPasswordHash());

    }

    // 프로필 수정 후 세션 유지 테스트
    @Test
    void sessionRemainsValidAfterProfileUpdate() throws Exception {
        // given
        MockHttpSession session = login(memberA.getLoginId());

        String newNickname = memberA.getLoginId() + "_new";

        //when
        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberA.getEmail(),
                        newNickname
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isOk());

        //then
        // 같은 세션으로 변경된 정보 조회
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(newNickname));
    }

    // 다른 회원이 프로필 수정 시 실패 테스트
    @Test
    void cannotUpdateAnotherMember() throws Exception {

        // given
        // 회원 A 로그인
        MockHttpSession session = login(memberA.getLoginId());

        //when
        // 회원 B의 프로필 수정 시도
        mockMvc.perform(
                updateRequest(
                        memberB.getId(),
                        "unauthorized@test.com",
                        "권한x"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_DENIED"));

        //then
        // 회원 B의 정보 변경 안된 것 확인
        Member unchanged = memberRepository.findById(memberB.getId())
                .orElseThrow();

        assertThat(unchanged.getEmail())
                .isEqualTo(memberB.getEmail());

        assertThat(unchanged.getNickname())
                .isEqualTo(memberB.getNickname());
    }

    // 존재하지 않는 회원 프로필 수정 시도 시 실패 테스트
    @Test
    void cannotUpdateUnknownMemberId() throws Exception {

        MockHttpSession session = login(memberA.getLoginId());

        mockMvc.perform(
                updateRequest(
                        Long.MAX_VALUE,
                        "unknown@test.com",
                        "존재x"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isForbidden());
    }

    // 비로그인으로 프로필 수정 시도 시 실패 테스트
    @Test
    void cannotUpdateWithoutAuthentication() throws Exception {

        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberA.getEmail(),
                        "비로그인"
                )
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());
    }

    private Member createMember(String loginId){

        return memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode(RAW_PASSWORD),
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    // 올바르지 않은 형식의 이메일 입력 시 실패 테스트
    @Test
    void rejectsInvalidEmail() throws Exception {

        MockHttpSession session = login(memberA.getLoginId());

        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        "invalid-email",
                        memberA.getEmail()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("email"));
    }


    // 중복되는 이메일 입력 시 실패 테스트
    @Test
    void rejectsDuplicateEmail() throws Exception {

        MockHttpSession session = login(memberA.getLoginId());

        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberB.getEmail(),
                        memberA.getNickname()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("email"));

    }

    // 중복되는 닉네임 입력 시 실패 테스트
    @Test
    void rejectsDuplicateNickname() throws Exception {

        MockHttpSession session = login(memberA.getLoginId());

        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberA.getEmail(),
                        memberB.getNickname()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("nickname"));

    }

    // 기존 이메일, 닉네임 입력해도 수정 성공 테스트
    @Test
    void acceptsCurrentEmailAndNickname() throws Exception {

        MockHttpSession session = login(memberA.getLoginId());

        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberA.getEmail(),
                        memberA.getNickname()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isOk());
    }

    @Test
    void cannotUpdateWithoutCsrfToken() throws Exception {

        // given
        MockHttpSession session = login(memberA.getLoginId());

        // when
        mockMvc.perform(
                updateRequest(
                        memberA.getId(),
                        memberA.getEmail(),
                        "권한 없는 수정"
                )
                        .session(session)
        )
                .andExpect(status().isForbidden());

        // then
        Member unchanged = memberRepository.findById(memberA.getId())
                .orElseThrow();

        assertThat(unchanged.getNickname()).isEqualTo(memberA.getNickname());
    }

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

    private MockHttpServletRequestBuilder updateRequest(
            Long memberId,
            String email,
            String nickname
    ){

        String requestBody = """
                {
                    "email": "%s",
                    "nickname": "%s"
                }
                """.formatted(email, nickname);

        return put("/api/members/{memberId}/profile", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
    }
}
