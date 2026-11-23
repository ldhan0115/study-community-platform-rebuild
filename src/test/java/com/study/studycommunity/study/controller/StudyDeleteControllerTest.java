package com.study.studycommunity.study.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StudyDeleteControllerTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member owner;
    private Member anotherMember;

    private Long studyId;
    private String studyTitle;

    @BeforeEach
    void setUp(){

        owner = createMember();
        anotherMember = createMember();

        studyTitle = "delete_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        Study study = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        studyTitle,
                        "논리 삭제 테스트",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        studyId = study.getId();

        entityManager.clear();

    }

    // 스터디 삭제 성공 테스트
    @Test
    void ownerCanSoftDeleteStudy() throws Exception {

        // given
        MockHttpSession session = login(owner);

        //when
        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();
        entityManager.clear();

        //then
        // 물리적으로 삭제되지 않았는지 확인
        Integer remainingRows = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM study
                        WHERE study_id = ?
                        """,
                Integer.class,
                studyId
        );

        assertThat(remainingRows).isEqualTo(1);

        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                """
                        SELECT deleted_at
                        FROM study
                        WHERE study_id = ?
                        """,
                LocalDateTime.class,
                studyId
        );

        assertThat(deletedAt).isNotNull();

        // 개설자 FK 유지 확인
        Long savedMemberId = jdbcTemplate.queryForObject(
                """
                        SELECT member_id
                        FROM study
                        WHERE study_id = ?
                        """,
                Long.class,
                studyId
        );

        assertThat(savedMemberId).isEqualTo(owner.getId());
    }

    // 삭제된 스터디는 목록/상세 조회에서 제외 테스트
    @Test
    void deletedStudyIsHiddenFromDetailAndList() throws Exception {

        // given
        MockHttpSession session = login(owner);

        mockMvc.perform(
                        delete("/api/studies/{studyId}", studyId)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        //when & then
        mockMvc.perform(
                get("/api/studies/{studyId}", studyId)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        mockMvc.perform(
                get("/api/studies")
                        .param("keyword", studyTitle)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }


    // 다른 회원의 삭제 요청 차단 테스트
    @Test
    void anotherMemberCannotDeleteStudy() throws Exception {

        // given
        MockHttpSession session = login(anotherMember);

        //when
        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDY_ACCESS_DENIED"));

        //then
        assertStudyNotDeleted();
    }

    // 비로그인 시 삭제 요청 차단 테스트
    @Test
    void cannotDeleteWithoutAuthentication() throws Exception {

        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());

        assertStudyNotDeleted();
    }

    // 5. csrf 토큰 없는 요청 차단 테스트
    @Test
    void cannotDeleteWithoutCsrfToken() throws Exception {

        MockHttpSession session = login(owner);

        mockMvc.perform(
                        delete("/api/studies/{studyId}", studyId)
                                .session(session)
                )
                .andExpect(status().isForbidden());

        assertStudyNotDeleted();
    }

    // 존재하지 않는 스터디 삭제 실패 테스트
    @Test
    void cannotDeleteUnknownStudy() throws Exception {

        // given
        MockHttpSession session = login(owner);

        // when
        mockMvc.perform(
                        delete("/api/studies/{studyId}",
                                Long.MAX_VALUE)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        // then
        assertStudyNotDeleted();
    }

    // 이미 삭제한 스터디 삭제 재요청 시 404 반환 테스트
    @Test
    void deletingSameStudyTwiceReturns404() throws Exception {

        // given
        MockHttpSession session = login(owner);

        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        LocalDateTime firstDeletedAt = findDeletionTime();

        //when
        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        entityManager.flush();

        //then
        // 삭제 시간이 바뀌지 않았는지 확인
        assertThat(findDeletionTime())
                .isEqualTo(firstDeletedAt);
    }

    // 모집 마감된 스터디도 삭제 가능 테스트
    @Test
    void canDeleteClosedStudy() throws Exception {

        // given
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET status = 'CLOSED'
                    WHERE study_id = ?
                    """,
                studyId
        );

        entityManager.clear();

        MockHttpSession session = login(owner);

        //when
        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        entityManager.flush();

        //then
        assertThat(findDeletionTime()).isNotNull();

        String status = jdbcTemplate.queryForObject(
                """
                        SELECT status
                        FROM study
                        WHERE study_id = ?
                        """,
                String.class,
                studyId
        );

        assertThat(status).isEqualTo("CLOSED");
    }

    // 삭제된 스터디 수정 요청 시 차단 테스트
    @Test
    void deletedStudyCannotBeUpdated() throws Exception {

        // given
        MockHttpSession session = login(owner);

        mockMvc.perform(
                delete("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        String requestBody = """
                {
                    "title": "수정 시도",
                    "description": "변경된 내용",
                    "method": "ONLINE",
                    "region": null,
                    "capacity": 5,
                    "status": "OPEN"
                }
                """;

        //when
        mockMvc.perform(
                put("/api/studies/{studyId}", studyId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        //then
        assertThat(findDeletionTime()).isNotNull();
    }

    // 스터디 삭제 후 로그인 세션 유지 테스트
    @Test
    void loginSessionRemainsValidAfterDeletion() throws Exception {

        // given
        MockHttpSession session = login(owner);

        // when
        mockMvc.perform(
                        delete("/api/studies/{studyId}", studyId)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        // then
        assertThat(session.isInvalid()).isFalse();

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(session)
                )
                .andExpect(status().isOk());
    }

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

    private MockHttpSession login(Member owner) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("loginId", owner.getLoginId())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    // 스터디가 삭제되지 않았는지 검증
    private void assertStudyNotDeleted(){

        entityManager.flush();
        entityManager.clear();

        Integer activeRows = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM study
                        WHERE study_id = ?
                        AND deleted_at IS NULL
                        """,
                Integer.class,
                studyId
        );

        assertThat(activeRows).isEqualTo(1);
    }

    // 삭제 시간 반환
    private LocalDateTime findDeletionTime(){

        return jdbcTemplate.queryForObject(
                """
                SELECT deleted_at
                FROM study
                WHERE study_id = ?
                """,
                LocalDateTime.class,
                studyId
        );
    }
}
