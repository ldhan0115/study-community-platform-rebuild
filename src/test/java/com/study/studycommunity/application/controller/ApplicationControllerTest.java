package com.study.studycommunity.application.controller;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;
import com.study.studycommunity.application.dto.ApplicationCreateRequest;
import com.study.studycommunity.application.repository.ApplicationRepository;
import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationControllerTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private Member owner;
    private Member applicant;
    private Member anotherApplicant;

    private Study study;

    @BeforeEach
    void setUp() {

        owner = createMember();
        applicant = createMember();
        anotherApplicant = createMember();

        study = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        "Spring 백엔드 스터디",
                        "Spring과 JPA를 함께 학습해요",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        entityManager.clear();
    }

    // 스터디 신청 성공 테스트
    @Test
    void memberCanApplyToOpenStudy() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "함께 공부하고 싶습니다."
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId")
                        .isNumber())
                .andExpect(jsonPath("$.studyId")
                        .value(study.getId()))
                .andExpect(jsonPath("$.memberId")
                        .value(applicant.getId()))
                .andExpect(jsonPath("$.message")
                        .value("함께 공부하고 싶습니다."))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.createdAt")
                        .isNotEmpty())
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        entityManager.flush();
        entityManager.clear();

        Application saved = applicationRepository
                .findByStudy_IdAndMember_Id(
                        study.getId(),
                        applicant.getId()
                )
                .orElseThrow();

        //then
        assertThat(saved.getStatus())
                .isEqualTo(ApplicationStatus.PENDING);

        assertThat(saved.getMessage())
                .isEqualTo("함께 공부하고 싶습니다.");

    }

    // 자신의 스터디에 신청 차단 테스트
    @Test
    void ownerCannotApplyToOwnStudy() throws Exception {

        // given
        MockHttpSession session = login(owner);

        //when
        mockMvc.perform(
                applicationRequest(
                        study.getId(),
                        "나의 스터디에 신청"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OWN_STUDY_APPLICATION"));

        //then
        assertNoApplication(owner);
    }

    // 마감된 스터디에 신청 시 실패 테스트
    @Test
    void cannotApplyToClosedStudy() throws Exception {

        // given
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET status = 'CLOSED'
                    WHERE study_id = ?
                    """,
                study.getId()
        );

        entityManager.clear();

        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                applicationRequest(
                        study.getId(),
                        "마감된 스터디 신청"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_OPEN"));

        //then
        assertNoApplication(applicant);
    }

    // 논리 삭제된 스터디 신청 차단 테스트
    @Test
    void cannotApplyToDeletedStudy() throws Exception {

        // given
        jdbcTemplate.update(
                """
                UPDATE study
                SET deleted_at = CURRENT_TIMESTAMP(6)
                WHERE study_id = ?
                """,
                study.getId()
        );

        entityManager.clear();

        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                applicationRequest(
                        study.getId(),
                        "삭제된 스터디 신청"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        //then
        assertNoApplication(applicant);
    }

    // 같은 스터디에 중복 신청 차단 테스트
    @Test
    void duplicateApplicationReturns409() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        mockMvc.perform(
                applicationRequest(
                        study.getId(),
                        "첫 번째 신청"
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isCreated());

        //when & then
        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "두 번째 신청"
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("DUPLICATE_APPLICATION"));
    }

    // 서로 다른 회원은 같은 스터디 신청 가능 테스트
    @Test
    void differentMembersCanApplyToSameStudy() throws Exception {

        // given
        MockHttpSession firstSession = login(applicant);
        MockHttpSession secondSession = login(anotherApplicant);

        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "첫 번째 회원 신청"
                        )
                                .session(firstSession)
                                .with(csrf())
                )
                .andExpect(status().isCreated());

        //when
        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "두 번째 회원 신청"
                        )
                                .session(secondSession)
                                .with(csrf())
                )
                .andExpect(status().isCreated());

        long count = applicationRepository
                .countByStudy_IdAndStatus(
                        study.getId(),
                        ApplicationStatus.PENDING
                );


        //then
        assertThat(count).isEqualTo(2L);
    }

    // 존재하지 않는 스터디에 신청 차단 테스트
    @Test
    void cannotApplyToUnknownStudy() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                        applicationRequest(
                                Long.MAX_VALUE,
                                "존재하지 않는 스터디 신청"
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        //then
        assertNoApplication(applicant);
    }

    @Test
    void rejectsBlankMessage() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                applicationRequest(study.getId(), "  ")
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("message"));

        //then
        assertNoApplication(applicant);
    }

    // 메시지 길이 초과 시 신청 실패 테스트
    @Test
    void rejectsMessageLongerThan255Characters() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "a".repeat(256)
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field")
                        .value("message"));

        //then
        assertNoApplication(applicant);
    }

    // 로그인 없이 신청 차단 테스트
    @Test
    void cannotApplyWithoutAuthentication() throws Exception {

        //when
        mockMvc.perform(
                applicationRequest(
                        study.getId(),
                        "로그인 하지 않은 회원 신청"
                )
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());

        //then
        assertNoApplication(applicant);
    }

    // csrf 토큰 누락 시 신청 차단
    @Test
    void cannotApplyWithoutCsrfToken() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when
        mockMvc.perform(
                        applicationRequest(
                                study.getId(),
                                "CSRF 토큰 없는 신청"
                        )
                                .session(session)
                )
                .andExpect(status().isForbidden());

        //then
        assertNoApplication(applicant);
    }


    // ------------------------
    // 테스트 공통 메서드
    // ------------------------

    private Member createMember(){

        String loginId = "mem_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

        return memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode(PASSWORD),
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    private MockHttpSession login(Member applicant) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("loginId", applicant.getLoginId())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    private MockHttpServletRequestBuilder applicationRequest(
            Long studyId,
            String message
    ){

        ApplicationCreateRequest request = new ApplicationCreateRequest(message);

        return post("/api/studies/{studyId}/applications", studyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request));
    }

    private void assertNoApplication(Member member){

        boolean exists = applicationRepository.existsByStudy_IdAndMember_Id(
                study.getId(),
                member.getId()
        );

        assertThat(exists).isFalse();
    }

}