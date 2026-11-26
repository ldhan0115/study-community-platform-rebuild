package com.study.studycommunity.application.controller;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.repository.ApplicationRepository;
import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApplicationListControllerTest {

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

    private Member owner;
    private Member applicant;
    private Member anotherApplicant;

    private Study study;
    private Study anotherStudy;

    private Application firstApplication;
    private Application secondApplication;

    @BeforeEach
    void setUp(){

        owner = createMember();
        applicant = createMember();
        anotherApplicant = createMember();

        study = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        "Spring 백엔드 스터디",
                        "Spring 학습",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        anotherStudy = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        "Java 알고리즘 스터디",
                        "알고리즘 학습",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        // 첫 번째 스터디에는 신청 2개
        firstApplication = createApplication(
                applicant,
                study,
                "Spring 스터디에 참여하고 싶습니다."
        );

        secondApplication = createApplication(
                anotherApplicant,
                study,
                "함께 공부하고 싶습니다."
        );

        // 두 번째 스터디에는 신청 1개
        createApplication(
                applicant,
                anotherStudy,
                "알고리즘 스터디에 참여하고 싶습니다."
        );

        entityManager.clear();

    }

    // 개설자가 자신의 스터디의 신청 목록 조회 성공 테스트
    @Test
    void ownerCanReadStudyApplications() throws Exception {

        // given
        MockHttpSession session = login(owner);

        //when & then
        mockMvc.perform(
                get("/api/studies/{studyId}/applications", study.getId())
                        .session(session)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].applicationId").isNumber())
                .andExpect(jsonPath("$.content[0].memberId").isNumber())
                .andExpect(jsonPath("$.content[0].applicantNickname").isNotEmpty())
                .andExpect(jsonPath("$.content[0].message").isNotEmpty())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    // 페이징 검증 테스트
    @Test
    void returnsCorrectPaginationMetadata() throws Exception {

        // given & then
        MockHttpSession session = login(owner);

        //when
        // 첫 번째 페이지
        mockMvc.perform(
                get("/api/studies/{studyId}/applications", study.getId())
                        .session(session)
                        .param("page", "0")
                        .param("size", "1")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                study.getId())
                                .session(session)
                                .param("page", "1")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    // 개설자가 아닌 회원의 목록 조회 차단 테스트
    @Test
    void nonOwnerCannotReadStudyApplications() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //when & then
        mockMvc.perform(
                get("/api/studies/{studyId}/applications", study.getId())
                        .session(session)
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDY_ACCESS_DENIED"));
    }

    // 비로그인 조회 차단 테스트
    @Test
    void cannotReadStudyApplicationsWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                study.getId())
                )
                .andExpect(status().isUnauthorized());
    }

    // 존재하지 않는 스터디 조회 시 차단 테스트
    @Test
    void unknownStudyReturns404() throws Exception {

        MockHttpSession session = login(owner);

        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                Long.MAX_VALUE)
                                .session(session)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));
    }

    // 삭제된 스터디 목록 조회에서 제외 테스트
    @Test
    void deletedStudyApplicationsReturn404() throws Exception {

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

        MockHttpSession session = login(owner);

        //when & then
        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                study.getId())
                                .session(session)
                )
                .andExpect(status().isNotFound());
    }

    // 개인은 본인이 제출한 신청만 조회 가능 테스트
    @Test
    void memberCanReadOnlyOwnApplications() throws Exception {
        // given

        MockHttpSession session = login(applicant);

        mockMvc.perform(
                        get("/api/applications/me")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].studyId")
                        .isNumber())
                .andExpect(jsonPath("$.content[0].studyTitle")
                        .isNotEmpty())
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.content[0].email")
                        .doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash")
                        .doesNotExist());

        //when
        // 다른 회원에게는 자신의 신청 1개만 보임
        MockHttpSession anotherSession = login(anotherApplicant);

        //then
        mockMvc.perform(
                        get("/api/applications/me")
                                .session(anotherSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 삭제된 스터디도 신청 이력에 유지 테스트
    @Test
    void ownApplicationHistoryIncludesDeletedStudy() throws Exception {

        // given
        jdbcTemplate.update(
                """
                UPDATE study
                SET deleted_at = CURRENT_TIMESTAMP(6)
                WHERE study_id = ?
                """,
                anotherStudy.getId()
        );

        entityManager.clear();

        MockHttpSession session = login(applicant);

        // when
        mockMvc.perform(
                        get("/api/applications/me")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.studyDeleted == true)]", hasSize(1)));
    }

    // 잘못된 페이지 번호 차단 테스트
    @Test
    void rejectsNegativePage() throws Exception {

        // given
        MockHttpSession session = login(owner);

        // when & then
        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                study.getId())
                                .session(session)
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_APPLICATION_QUERY"));
    }

    //  잘못된 페이지 크기 차단 테스트
    @Test
    void rejectsInvalidPageSize() throws Exception {

        // given
        MockHttpSession session = login(applicant);

        //  when & then
        mockMvc.perform(
                        get("/api/applications/me")
                                .session(session)
                                .param("size", "51")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_APPLICATION_QUERY"));
    }

    // 잘못된 정렬 값 차단 테스트
    @Test
    void rejectsInvalidSort() throws Exception {

        // given
        MockHttpSession session = login(owner);

        // when & then
        mockMvc.perform(
                        get("/api/studies/{studyId}/applications",
                                study.getId())
                                .session(session)
                                .param("sort", "random")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_APPLICATION_QUERY"));
    }


    // ------------------------
    // 공통 테스트 메서드
    // ------------------------

    private Member createMember() {

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

    private Application createApplication(
            Member applicant,
            Study study,
            String message
    ){

        return applicationRepository.saveAndFlush(
                Application.create(
                        applicant,
                        study,
                        message
                )
        );
    }

    private MockHttpSession login(Member member) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("loginId", member.getLoginId())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }
}
