package com.study.studycommunity.study.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.dto.StudyUpdateRequest;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StudyUpdateControllerTest {

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

    @Autowired
    private JsonMapper jsonMapper;

    private Member owner;
    private Member anotherMember;
    private Study study;

    @BeforeEach
    void setUp(){

        owner = createMember();
        anotherMember = createMember();

        study = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        "Spring 스터디",
                        "Spring 기초 학습",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        entityManager.clear();
    }

    // 스터디 개설자 스터디 수정 성공 테스트
    @Test
    void ownerCanUpdateStudy() throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        LocalDateTime originalCreatedAt = study.getCreatedAt();

        //when
        mockMvc.perform(
                updateRequest(
                        study.getId(),
                        validUpdateRequest()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId")
                        .value(study.getId()))
                .andExpect(jsonPath("$.memberId")
                        .value(owner.getId()))
                .andExpect(jsonPath("$.title")
                        .value("Spring 심화 스터디"))
                .andExpect(jsonPath("$.method")
                        .value("OFFLINE"))
                .andExpect(jsonPath("$.region")
                        .value("서울"))
                .andExpect(jsonPath("$.capacity")
                        .value(8))
                .andExpect(jsonPath("$.status")
                        .value("CLOSED"))
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.email")
                        .doesNotExist());

        //then
        entityManager.flush();
        entityManager.clear();

        Study updated = findStudy();

        assertThat(updated.getTitle())
                .isEqualTo("Spring 심화 스터디");

        assertThat(updated.getDescription())
                .isEqualTo("Spring Security와 JPA 학습");

        assertThat(updated.getMethod())
                .isEqualTo(StudyMethod.OFFLINE);

        assertThat(updated.getRegion())
                .isEqualTo("서울");

        assertThat(updated.getCapacity())
                .isEqualTo(8);

        assertThat(updated.getStatus())
                .isEqualTo(StudyStatus.CLOSED);

        assertThat(updated.getMember().getId())
                .isEqualTo(owner.getId());

        // 생성 시간은 유지
        assertThat(updated.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        assertThat(updated.getUpdatedAt()).isNotNull();

        // 프로필 수정과 마찬가지로 세션 유지
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
                .andExpect(status().isOk());
    }

    // 다른 회원이 스터디 수정하면 실패 테스트
    @Test
    void anotherMemberCannotUpdateStudy() throws Exception {

        // given
        MockHttpSession session = login(anotherMember.getLoginId());

        //when
        mockMvc.perform(
                        updateRequest(
                                study.getId(),
                                validUpdateRequest()
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_ACCESS_DENIED"));

        //then
        assertOriginalStudyUnchanged();
    }

    // 로그인 없이 스터디 수정 실패 테스트
    @Test
    void cannotUpdateWithoutAuthentication() throws Exception {

        mockMvc.perform(
                        updateRequest(
                                study.getId(),
                                validUpdateRequest()
                        )
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());

        assertOriginalStudyUnchanged();
    }

    // csrf 토큰 없이 스터디 수정 실패 테스트
    @Test
    void cannotUpdateWithoutCsrfToken() throws Exception {

        MockHttpSession session = login(owner.getLoginId());

        mockMvc.perform(
                        updateRequest(
                                study.getId(),
                                validUpdateRequest()
                        )
                                .session(session)
                )
                .andExpect(status().isForbidden());

        assertOriginalStudyUnchanged();

    }

    // 존재하지 않는 스터디 수정 실패 테스트
    @Test
    void cannotUpdateUnknownStudy() throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        //when
        mockMvc.perform(
                        updateRequest(
                                Long.MAX_VALUE,
                                validUpdateRequest()
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        //then
        assertOriginalStudyUnchanged();

    }

    @Test
    void cannotUpdateDeletedStudy() throws Exception {

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

        MockHttpSession session = login(owner.getLoginId());

        //when & then
        mockMvc.perform(
                        updateRequest(
                                study.getId(),
                                validUpdateRequest()
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        // 삭제 상태 유지
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                """
                        SELECT deleted_at
                        FROM study
                        WHERE study_id = ?
                        """,
                LocalDateTime.class,
                study.getId()
        );

        assertThat(deletedAt).isNotNull();
    }

    // 스터디 제목 공백 시 수정 실패 테스트
    @Test
    void rejectsBlankTitle() throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        StudyUpdateRequest request = new StudyUpdateRequest(
                "",
                "수정된 내용",
                StudyMethod.ONLINE,
                null,
                5,
                StudyStatus.OPEN
        );

        // when
        mockMvc.perform(
                        updateRequest(study.getId(), request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field")
                        .value("title"));

        // then
        assertOriginalStudyUnchanged();
    }

    // 온라인 스터디에 지역 값 입력 시 실패 테스트
    @Test
    void rejectsRegionForOnlineStudy() throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        StudyUpdateRequest request = new StudyUpdateRequest(
                "Spring 심화 스터디",
                "수정된 내용",
                StudyMethod.ONLINE,
                "서울",
                5,
                StudyStatus.OPEN
        );

        // when
        mockMvc.perform(
                        updateRequest(study.getId(), request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_STUDY"))
                .andExpect(jsonPath("$.field")
                        .value("region"));

        // then
        assertOriginalStudyUnchanged();
    }

    // 오프라인 스터디에 지역 값 누락 시 실패 테스트
    @Test
    void rejectsMissingRegionForOfflineStudy()
            throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        StudyUpdateRequest request = new StudyUpdateRequest(
                "Spring 심화 스터디",
                "수정된 내용",
                StudyMethod.OFFLINE,
                null,
                5,
                StudyStatus.OPEN
        );

        // when
        mockMvc.perform(
                        updateRequest(study.getId(), request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_STUDY"))
                .andExpect(jsonPath("$.field")
                        .value("region"));

        // then
        assertOriginalStudyUnchanged();
    }

    // 유요하지 않은 모집 인원 입력 시 수정 실패 테스트
    @ParameterizedTest
    @ValueSource(ints = {0, 31})
    void rejectsInvalidCapacity(int capacity) throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        StudyUpdateRequest request = new StudyUpdateRequest(
                "Spring 심화 스터디",
                "수정된 내용",
                StudyMethod.ONLINE,
                null,
                capacity,
                StudyStatus.OPEN
        );

        // when
        mockMvc.perform(
                        updateRequest(study.getId(), request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field")
                        .value("capacity"));

        // then
        assertOriginalStudyUnchanged();
    }

    // 모집 상태 누락 시 수정 실패 테스트
    @Test
    void rejectsMissingStatus() throws Exception {

        // given
        MockHttpSession session = login(owner.getLoginId());

        StudyUpdateRequest request = new StudyUpdateRequest(
                "Spring 심화 스터디",
                "수정된 내용",
                StudyMethod.ONLINE,
                null,
                5,
                null
        );

        // when
        mockMvc.perform(
                        updateRequest(study.getId(), request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field")
                        .value("status"));

        // then
        assertOriginalStudyUnchanged();
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

    // 수정 요청에 사용할 인증된 DTO
    private StudyUpdateRequest validUpdateRequest(){

        return new StudyUpdateRequest(
                "Spring 심화 스터디",
                "Spring Security와 JPA 학습",
                StudyMethod.OFFLINE,
                "서울",
                8,
                StudyStatus.CLOSED
        );
    }

    private MockHttpSession login(String loginId) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("loginId", loginId)
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;

    }

    // DB에서 최신 Study 상태 조회
    private Study findStudy(){

        return studyRepository.findDetailById(study.getId())
                .orElseThrow();
    }

    // Java DTO를 JSON으로 변환한 수정 요청
    private MockHttpServletRequestBuilder updateRequest(
            Long studyId,
            StudyUpdateRequest request
    ){

        return put("/api/studies/{studyId}", studyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request));

    }

    // 수정 실패 후 기존 상태가 보존되었는지 검증
    private void assertOriginalStudyUnchanged(){

        entityManager.flush();
        entityManager.clear();

        Study unchanged = findStudy();

        assertThat(unchanged.getTitle())
                .isEqualTo("Spring 스터디");

        assertThat(unchanged.getDescription())
                .isEqualTo("Spring 기초 학습");

        assertThat(unchanged.getMethod())
                .isEqualTo(StudyMethod.ONLINE);

        assertThat(unchanged.getRegion()).isNull();

        assertThat(unchanged.getCapacity())
                .isEqualTo(5);

        assertThat(unchanged.getStatus())
                .isEqualTo(StudyStatus.OPEN);

        assertThat(unchanged.getMember().getId())
                .isEqualTo(owner.getId());
    }
}
