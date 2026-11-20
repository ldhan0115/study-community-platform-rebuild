package com.study.studycommunity.study.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class StudyDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member member;
    private Study study;

    @BeforeEach
    void setUp(){

        String loginId = "mem_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

        member = memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode("password123!"),
                        loginId + "@test.com",
                        loginId
                )
        );

        study = studyRepository.saveAndFlush(
                Study.create(
                        member,
                        "Spring 스터디",
                        "Spring을 함께 공부해요",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );
    }

    @AfterEach
    void tearDown(){

        if(study != null){
            studyRepository.deleteById(study.getId());
        }

        if(member != null){
            memberRepository.deleteById(member.getId());
        }
    }

    // 로그인 없이 스터디 상세 조회 가능 테스트
    @Test
    void getsStudyDetailWithoutAuthentication() throws Exception {

        mockMvc.perform(
                get("/api/studies/{studyId}",
                study.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId")
                        .value(study.getId()))
                .andExpect(jsonPath("$.memberId")
                        .value(member.getId()))
                .andExpect(jsonPath("$.ownerNickname")
                        .value(member.getNickname()))
                .andExpect(jsonPath("$.title")
                        .value("Spring 스터디"))
                .andExpect(jsonPath("$.description")
                        .value("Spring을 함께 공부해요"))
                .andExpect(jsonPath("$.method")
                        .value("ONLINE"))
                .andExpect(jsonPath("$.capacity")
                        .value(5))
                .andExpect(jsonPath("$.status")
                        .value("OPEN"))
                .andExpect(jsonPath("$.createdAt")
                        .isNotEmpty());
    }

    // 스터디 조회 시 중요 정보 미포함 테스트
    @Test
    void doesNotExposeSensitiveMemberInformation() throws Exception {

        mockMvc.perform(
                get("/api/studies/{studyId}",
                        study.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password")
                        .doesNotExist())
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.email")
                        .doesNotExist())
                .andExpect(jsonPath("$.loginId")
                        .doesNotExist())
                .andExpect(jsonPath("$.member")
                        .doesNotExist());
    }

    // 존재하지 않는 스터디 조회 시 404에러 반환 테스트
    @Test
    void returns404WhenStudyDoesNotExist() throws Exception {

        mockMvc.perform(
                get("/api/studies/{studyId}",
                        Long.MAX_VALUE)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    // 삭제된 스터디 조회 시 404에러 반환 테스트
    @Test
    void returns404WhenStudyIsDeleted() throws Exception {

        // given
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET deleted_at = CURRENT_TIMESTAMP(6)
                    WHERE study_id = ?
                    """,
                study.getId()
        );

        //when & then
        mockMvc.perform(
                get("/api/studies/{studyId}",
                        study.getId())
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }


    // 모집 상태 마감인 스터디 조회 가능 테스트
    @Test
    void canReadClosedStudy() throws Exception {

        // given
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET status = 'CLOSED'
                    WHERE study_id = ?
                    """,
                study.getId()
        );

        //when & then
        mockMvc.perform(
                get("/api/studies/{studyId}",
                        study.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // 올바르지 않은 URL 변수로 스터디 조회 실패 테스트
    @Test
    void rejectsInvalidStudyId() throws Exception {

        mockMvc.perform(
                get("/api/studies/abc")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("studyId"));
    }

    // 비로그인 시 스터디 생성 불가 재확인
    @Test
    void studyCreationStillRequiresAuthentication() throws Exception {

        // given
        String requestBody = """
                {
                    "title": "Spring 스터디",
                    "description": "스터디 내용",
                    "method": "ONLINE",
                    "region": null,
                    "capacity": 5
                }
                """;

        //when & then
        mockMvc.perform(
                post("/api/studies")
                        .with(csrf())
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(requestBody)
        )
                .andExpect(status().isUnauthorized());
    }

}
