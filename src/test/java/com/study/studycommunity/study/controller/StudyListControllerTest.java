package com.study.studycommunity.study.controller;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StudyListControllerTest {

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

    @Autowired
    private EntityManager entityManager;

    private Study springIntro;
    private Study javaBasic;
    private Study springOffline;
    private Study cppAlgorithm;
    private Study springCloud;

    @BeforeEach
    void setUp(){

        Member ownerA = createMember("ownerA");
        Member ownerB = createMember("ownerB");
        Member ownerC = createMember("ownerC");

        springIntro = createStudy(
                ownerA,
                "Spring 입문",
                StudyMethod.ONLINE,
                null
        );

        javaBasic = createStudy(
                ownerA,
                "Java 기초",
                StudyMethod.ONLINE,
                null
        );

        springOffline = createStudy(
                ownerB,
                "Spring 오프라인",
                StudyMethod.OFFLINE,
                "서울"
        );

        cppAlgorithm = createStudy(
                ownerB,
                "C++ 알고리즘",
                StudyMethod.OFFLINE,
                "수원"
        );

        springCloud = createStudy(
                ownerC,
                "Spring Cloud",
                StudyMethod.ONLINE,
                null
        );

        Study deleted = createStudy(
                ownerC,
                "삭제된 Spring",
                StudyMethod.ONLINE,
                null
        );

        // 모집 마감 상태 설정
        jdbcTemplate.update(
                "UPDATE study SET status = 'CLOSED' WHERE study_id = ?",
                springOffline.getId()
        );

        // 논리 삭제 상태 설정
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET deleted_at = CURRENT_TIMESTAMP(6)
                    WHERE study_id = ?
                    """,
                deleted.getId()
        );

        // 직접 설정한 UPDATE 반영 및 다시 조회하도록 함
        entityManager.clear();
    }

    @AfterEach
    public void delete(){
        jdbcTemplate.update("""
                DELETE FROM study
                """);
    }

    // 로그인 없이 목록 조회 가능 테스트
    @Test
    void getsStudyListWithoutLogin() throws Exception {

        mockMvc.perform(
                get("/api/studies")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    // 스터디 목록 제목 검색 테스트
    @Test
    void searchesByTitleKeyword() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("keyword", "spring")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    // 스터디 목록 진행 방식 검색 테스트
    @Test
    void filtersByStudyMethod() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("method", "OFFLINE")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].method").value("OFFLINE"))
                .andExpect(jsonPath("$.content[1].method").value("OFFLINE"));
    }

    // 스터디 목록 모집 상태 검색 테스트
    @Test
    void filtersByStudyStatus() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("status", "CLOSED")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studyId").value(springOffline.getId()));
    }

    // 스터디 목록 지역 검색 테스트
    @Test
    void searchesByRegion() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("region", "서울")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studyId").value(springOffline.getId()));
    }

    // 스터디 목록 복합 조건 검색 테스트
    @Test
    void combinesMultipleSearchConditions() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("keyword", "Spring")
                        .param("method", "ONLINE")
                        .param("status", "OPEN")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void returnsCorrectPaginationMetadata() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("page", "0")
                        .param("size", "2")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(
                get("/api/studies")
                        .param("page", "2")
                        .param("size", "2")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    // 스터디 목록 오래된 순 정렬 테스트
    @Test
    void sortsByOldest() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("sort", "oldest")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studyId").value(springIntro.getId()))
                .andExpect(jsonPath("$.content[1].studyId").value(javaBasic.getId()));
    }

    // 삭제된 스터디 검색 시 제외 테스트
    @Test
    void excludesDeletedStudy() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("keyword", "삭제된 Spring")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // 유효하지 않는 페이지 사이즈 요청 시 실패 테스트
    @Test
    void rejectsInvalidPageSize() throws Exception {

         mockMvc.perform(
                 get("/api/studies")
                         .param("size", "51")
         )
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                 .andExpect(jsonPath("$.field").value("size"));
    }

    // 음수 페이지 요청 시 거절 테스트
    @Test
    void rejectsNegativePage() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("page", "-1")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("page"));
    }

    // 유요하지 않은 정렬 방식 요청 시 거절 테스트
    @Test
    void rejectsInvalidSort() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("sort", "random")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("sort"));
    }

    // 존재하지 않는 모집 방식 요청 시 실패 테스트
    @Test
    void rejectsUnknownMethod() throws Exception {

        mockMvc.perform(
                get("/api/studies")
                        .param("method", "UNKNOWN")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("method"));
    }

    // 스터디 목록 조회 시 민감한 정보 미포함 테스트
    @Test
    void doesNotExposePrivateMemberData() throws Exception {

        mockMvc.perform(
                get("/api/studies")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownerNickname").isNotEmpty())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    // 스터디 생성 시 로그인 필요 테스트
    @Test
    void studyCreationStillRequiresLogin() throws Exception {

        mockMvc.perform(
                post("/api/studies")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}")
        )
                .andExpect(status().isUnauthorized());
    }

    private Member createMember(String prefix){

        String loginId = prefix + "_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        return memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode("password123!"),
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    private Study createStudy(
            Member owner,
            String title,
            StudyMethod method,
            String region
    ){

        return studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        title,
                        "스터디 내용",
                        method,
                        region,
                        5
                )
        );
    }

}
