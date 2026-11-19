package com.study.studycommunity.study.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import com.study.studycommunity.study.dto.StudyCreateRequest;
import com.study.studycommunity.study.repository.StudyRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudyControllerTest {

    private static final String RAW_PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member member;

    @BeforeEach
    void setUp(){

        String loginId = "mem_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

        member = memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode(RAW_PASSWORD),
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    @AfterEach
    void tearDown(){

        if(member != null){

            // FK 관계 때문에 자식 데이터인 Study 먼저 삭제
            jdbcTemplate.update(
                    "DELETE FROM study WHERE member_id = ?",
                    member.getId()
            );

            memberRepository.deleteById(member.getId());
        }
    }

    // 온라인 스터디 개설 성공 테스트
    @Test
    void createsOnlineStudy() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Spring 스터디",
                "Spring을 함께 공부해요",
                StudyMethod.ONLINE,
                null,
                5
        );

        //when
        MvcResult result = mockMvc.perform(
                        createRequest(request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studyId").isNumber())
                .andExpect(jsonPath("$.memberId")
                        .value(member.getId()))
                .andExpect(jsonPath("$.ownerNickname")
                        .value(member.getNickname()))
                .andExpect(jsonPath("$.title")
                        .value("Spring 스터디"))
                .andExpect(jsonPath("$.method")
                        .value("ONLINE"))
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn();

        //then
        // 실제 DB 저장 확인
        List<Study> studies = studyRepository.findByMember_IdAndDeletedAtIsNull(
                member.getId()
        );

        assertThat(studies).hasSize(1);

        Study saved = studies.getFirst();

        assertThat(saved.getTitle()).isEqualTo("Spring 스터디");
        assertThat(saved.getMethod()).isEqualTo(StudyMethod.ONLINE);
        assertThat(saved.getRegion()).isNull();
        assertThat(saved.getStatus()).isEqualTo(StudyStatus.OPEN);
        assertThat(saved.getCapacity()).isEqualTo(5);

        // 개설자 FK 확인
        Long savedMemberId = jdbcTemplate.queryForObject(
                """
                        SELECT member_id
                        FROM study
                        WHERE study_id = ?
                        """,
                Long.class,
                saved.getId()
        );

        assertThat(savedMemberId).isEqualTo(member.getId());

        // 생성된 리소스 위치 확인
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/studies/" + saved.getId());

    }

    // 오프라인 스터디 개설 성공 테스트
    @Test
    void createsOfflineStudy() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Java 스터디",
                "Java를 함께 공부해요",
                StudyMethod.OFFLINE,
                "서울",
                4
        );

        //when & then
        mockMvc.perform(
                createRequest(request)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("OFFLINE"))
                .andExpect(jsonPath("$.region").value("서울"))
                .andExpect(jsonPath("$.status").value("OPEN"));


        List<Study> studies = studyRepository.findByMember_IdAndDeletedAtIsNull(
                member.getId()
        );

        assertThat(studies).hasSize(1);
        assertThat(studies.getFirst().getRegion()).isEqualTo("서울");
    }

    // 스터디 제목 공백 시 개설 실패 테스트
    @Test
    void rejectsBlankTitle() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                5
        );

        //when & then
        mockMvc.perform(
                createRequest(request)
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("title"));

        assertNoStudyCreated();
    }

    // 모집 인원 요건 미충족시 스터디 개설 실패 테스트
    @ParameterizedTest
    @ValueSource(ints = {0, 31})
    void rejectsInvalidCapacity(int capacity) throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                capacity
        );


        //when & then
        mockMvc.perform(
                        createRequest(request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("capacity"));

        assertNoStudyCreated();
    }

    // 모집 인원 누락 시 스터디 개설 실패 테스트
    @Test
    void rejectsMissingCapacity() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                null
        );

        // when & then
        mockMvc.perform(
                        createRequest(request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("capacity"));

        assertNoStudyCreated();

    }

    // 온라인 스터디 개설 시 지역 입력하면 실패 테스트
    @Test
    void rejectsRegionForOnlineStudy() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                "서울",
                5
        );

        // when & then
        mockMvc.perform(
                        createRequest(request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STUDY"))
                .andExpect(jsonPath("$.field").value("region"));

        assertNoStudyCreated();
    }

    // 오프라인 스터디 개설 시 지역 누락하면 실패 테스트
    @Test
    void rejectsMissingRegionForOfflineStudy() throws Exception {

        // given
        MockHttpSession session = login();

        StudyCreateRequest request = new StudyCreateRequest(
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.OFFLINE,
                null,
                5
        );

        // when & then
        mockMvc.perform(
                        createRequest(request)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STUDY"))
                .andExpect(jsonPath("$.field").value("region"));

        assertNoStudyCreated();

    }

    // 유용하지 않은 진행 방식 입력 시 실패 테스트
    @Test
    void rejectsInvalidStudyMethod() throws Exception {

        // given
        MockHttpSession session = login();

        String requestBody = """
                {
                    "title": "Spring 스터디",
                    "description": "스터디 내용",
                    "method": "UNKNOWN",
                    "region": null,
                    "capacity": 5
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/studies")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        assertNoStudyCreated();
    }

    // 로그인 없이 스터디 개설 시 실패 테스트
    @Test
    void cannotCreateWithoutAuthentication() throws Exception {

        mockMvc.perform(
                createRequest(validRequest())
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());

        assertNoStudyCreated();
    }

    // csrf 토큰 없이 스터디 생성 시 실패 테스트
    @Test
    void cannotCreateWithoutCsrfToken() throws Exception {

        MockHttpSession session = login();

        mockMvc.perform(
                createRequest(validRequest())
                        .session(session)
        )
                .andExpect(status().isForbidden());

        assertNoStudyCreated();
    }

    // 정상적인 스터디 등록 요청 DTO
    private StudyCreateRequest validRequest(){

        return new StudyCreateRequest(
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                5
        );
    }

    // DTO를 JSON으로 변환하여 MockMvc 요청 생성
    private MockHttpServletRequestBuilder createRequest(
            StudyCreateRequest request
    ){

        return post("/api/studies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request));
    }

    private MockHttpSession login() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param("loginId", member.getLoginId())
                                .param("password", RAW_PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    // 스터디 생성 실패 확인
    private void assertNoStudyCreated(){

        assertThat(
                studyRepository.findByMember_IdAndDeletedAtIsNull(
                        member.getId()
                )
        ).isEmpty();
    }
}