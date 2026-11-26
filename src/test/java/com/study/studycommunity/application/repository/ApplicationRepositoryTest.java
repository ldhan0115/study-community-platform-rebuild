package com.study.studycommunity.application.repository;

import com.study.studycommunity.application.domain.Application;
import com.study.studycommunity.application.domain.ApplicationStatus;
import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.AssertionsForClassTypes;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

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
                        "Spring Security와 JPA를 공부해요",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );
    }

    // 신청 성공 테스트
    @Test
    void savesAndFindsPendingApplication(){

        // given
        Application application = createApplication(
                applicant,
                study,
                "참가하고 싶습니다."
        );

        Long applicationId = application.getId();

        //when
        entityManager.clear();

        Application found = applicationRepository
                .findById(applicationId)
                .orElseThrow();

        //then
        AssertionsForClassTypes.assertThat(found.getId()).isEqualTo(applicationId);

        AssertionsForClassTypes.assertThat(found.getMember().getId())
                .isEqualTo(applicant.getId());

        AssertionsForClassTypes.assertThat(found.getStudy().getId())
                .isEqualTo(study.getId());
    }

    // 스터디 상태(Enum)가 String으로 저장되는지 확인 테스트
    @Test
    void storesStatusAsString(){

        // given
        Application application = createApplication(
                applicant,
                study,
                "참가하고 싶습니다."
        );

        //when
        String status = jdbcTemplate.queryForObject(
                """
                        SELECT status
                        FROM application
                        WHERE application_id = ?
                        """,
                String.class,
                application.getId()
        );

        //then
        AssertionsForClassTypes.assertThat(status).isEqualTo("PENDING");
    }


    // Member 객체 지연로딩 검증 테스트
    @Test
    void loadsMemberAndStudyLazily(){

        // given
        Long applicationId = createApplication(
                applicant,
                study,
                "참가하고 싶습니다."
        ).getId();

        entityManager.clear();

        //when
        Application found = applicationRepository.findById(applicationId)
                .orElseThrow();

        //then
        AssertionsForClassTypes.assertThat(Hibernate.isInitialized(found.getMember())).isFalse();

        AssertionsForClassTypes.assertThat(Hibernate.isInitialized(found.getStudy())).isFalse();

        AssertionsForClassTypes.assertThat(found.getMember().getNickname())
                .isEqualTo(applicant.getNickname());

        AssertionsForClassTypes.assertThat(found.getStudy().getTitle())
                .isEqualTo("Spring 백엔드 스터디");

        AssertionsForClassTypes.assertThat(Hibernate.isInitialized(found.getMember()))
                .isTrue();

        AssertionsForClassTypes.assertThat(Hibernate.isInitialized(found.getStudy()))
                .isTrue();
    }

    // 신청 내역 조회 작동 테스트
    @Test
    void detectsExistingApplication(){

        // given
        createApplication(
                applicant,
                study,
                "참가 신청합니다."
        );

        //when
        boolean exists = applicationRepository.existsByStudy_IdAndMember_Id(
                study.getId(),
                applicant.getId()
        );

        boolean anotherExists = applicationRepository.existsByStudy_IdAndMember_Id(
                study.getId(),
                anotherApplicant.getId()
        );

        //then
        AssertionsForClassTypes.assertThat(exists).isTrue();
        AssertionsForClassTypes.assertThat(anotherExists).isFalse();

        Application found = applicationRepository.findByStudy_IdAndMember_Id(
                        study.getId(),
                        applicant.getId()
                )
                .orElseThrow();

        AssertionsForClassTypes.assertThat(found.getMember().getId())
                .isEqualTo(applicant.getId());

    }

    // 동일 스터디에 여러명이 신청 가능한지 확인 테스트
    @Test
    void allowsDifferentMembersToApplyToSameStudy() {

        // given
        Application first = createApplication(
                applicant,
                study,
                "첫 번째 참가 신청"
        );

        Application second = createApplication(
                anotherApplicant,
                study,
                "두 번째 참가 신청"
        );

        // when
        List<Application> applications = applicationRepository
                .findByStudy_IdOrderByCreatedAtDescIdDesc(
                        study.getId()
                );

        // then
        assertThat(applications).hasSize(2);

        assertThat(applications)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId()
                );

    }

    // 같은 회원이 여러 스터디에 신청 가능 테스트
    @Test
    void allowsMemberToApplyToDifferentStudies(){

        // given
        Study anotherStudy = studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        "Java 알고리즘 스터디",
                        "알고리즘을 공부합니다.",
                        StudyMethod.ONLINE,
                        null,
                        4
                )
        );

        Application first = createApplication(
                applicant,
                study,
                "Spring 신청"
        );

        Application second = createApplication(
                applicant,
                anotherStudy,
                "Java 신청"
        );


        //when
        List<Application> applications = applicationRepository
                .findByMember_IdOrderByCreatedAtDescIdDesc(
                        applicant.getId()
                );

        //then
        assertThat(applications).hasSize(2);

        assertThat(applications)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId()
                );
    }

    // 상태별 신청 수 확인 테스트
    @Test
    void countsApplicationsByStatus(){

        // given
        createApplication(
                applicant,
                study,
                "첫 번째 신청"
        );

        createApplication(
                anotherApplicant,
                study,
                "두 번째 신청"
        );

        //when
        long pendingCount = applicationRepository
                .countByStudy_IdAndStatus(
                        study.getId(),
                        ApplicationStatus.PENDING
                );

        long approvedCount = applicationRepository
                .countByStudy_IdAndStatus(
                        study.getId(),
                        ApplicationStatus.APPROVED
                );


        //then
        assertThat(pendingCount).isEqualTo(2L);
        assertThat(approvedCount).isZero();
    }

    // 동일 회원이 동일 스터디에 신청 시 차단 테스트 -> DB 유니크 제약 조건
    @Test
    void databaseRejectsDuplicateApplication(){

        // given
        createApplication(
                applicant,
                study,
                "첫 번째 신청"
        );

        //when & then
        assertThatThrownBy(() ->
                applicationRepository.saveAndFlush(
                        Application.create(
                                applicant,
                                study,
                                "두 번째 신청"
                        )
                )
        )
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 존재하지 않는 회원이 신청 시 차단 테스트 -> DB FK 제약 조건
    @Test
    void databaseRejectsUnknownMember(){

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO application(
                            study_id,
                            member_id,
                            message,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?, ?, ?, 'PENDING',
                            CURRENT_TIMESTAMP(6),
                            CURRENT_TIMESTAMP(6)
                        )
                        """,
                        study.getId(),
                        Long.MAX_VALUE,
                        "존재하지 않는 회원"
                )
        )
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 존재하지 않는 스터디에 신청 시 차단 테스트
    @Test
    void databaseRejectsUnknownStudy() {

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO application (
                            study_id,
                            member_id,
                            message,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?, ?, ?, 'PENDING',
                            CURRENT_TIMESTAMP(6),
                            CURRENT_TIMESTAMP(6)
                        )
                        """,
                        Long.MAX_VALUE,
                        applicant.getId(),
                        "존재하지 않는 스터디"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 개설자용 스터디 신청 목록 조회 시 신청자 함께 로딩되는지 확인 테스트
    @Test
    void studyApplicationPageLoadsMemberWithEntityGraph(){

        // given
        createApplication(
                applicant,
                study,
                "참가 신청합니다."
        );

        entityManager.clear();

        // when
        Page<Application> result = applicationRepository.findPageByStudyId(
                study.getId(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);

        Application found = result.getContent().get(0);

        assertThat(Hibernate.isInitialized(found.getMember())).isTrue();

    }

    // 내 신청 목록 조회 시 해당 스터디 함께 로딩 되는지 확인 테스트
    @Test
    void myApplicationPageLoadsStudiesWithEntityGraph() {

        // given
        createApplication(
                applicant,
                study,
                "참가 신청합니다."
        );

        entityManager.clear();

        // when
        Page<Application> result =
                applicationRepository.findPageByMemberId(
                        applicant.getId(),
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result.getContent()).hasSize(1);

        Application found = result.getContent().get(0);

        assertThat(Hibernate.isInitialized(found.getStudy()))
                .isTrue();
    }

    private Member createMember() {

        String loginId = "mem" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);

        return memberRepository.saveAndFlush(
                Member.create(
                        loginId,
                        passwordEncoder.encode("password123!"),
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    private Application createApplication(
            Member member,
            Study targetStudy,
            String message
    ) {

        return applicationRepository.saveAndFlush(
                Application.create(
                        member,
                        targetStudy,
                        message
                )
        );
    }

}

