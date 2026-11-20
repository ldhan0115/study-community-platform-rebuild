package com.study.studycommunity.study.repository;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import com.study.studycommunity.study.domain.StudyStatus;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class StudyRepositoryTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member member;

    @BeforeEach
    void setUp(){
        member = createMember();
    }

    // 온라인 스터디 저장 및 조회 테스트
    @Test
    void saveAndFindsOnlineStudy(){

        // given
        Study study = Study.create(
                member,
                "Spring 스터디",
                "Spring을 함께 공부해요",
                StudyMethod.ONLINE,
                null,
                5
        );

        //when
        Study saved = studyRepository.saveAndFlush(study);

        Long studyId = saved.getId();
        Long memberId = member.getId();

        entityManager.clear();

        Study found = studyRepository.findById(studyId)
                .orElseThrow();

        //then
        assertThat(found.getId()).isEqualTo(studyId);
        assertThat(found.getMember().getId()).isEqualTo(memberId);
        assertThat(found.getTitle()).isEqualTo("Spring 스터디");
        assertThat(found.getDescription()).isEqualTo("Spring을 함께 공부해요");
        assertThat(found.getMethod()).isEqualTo(StudyMethod.ONLINE);
        assertThat(found.getRegion()).isNull();
        assertThat(found.getCapacity()).isEqualTo(5);
        assertThat(found.getStatus()).isEqualTo(StudyStatus.OPEN);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
    }

    // 오프라인 스터디 저장 및 조회 테스트
    @Test
    void savesOfflineStudy(){

        // given
        Study study = Study.create(
                member,
                "Java 스터디",
                "Java를 함께 공부합니다.",
                StudyMethod.OFFLINE,
                "서울",
                5
        );

        //when
        Long studyId = studyRepository.saveAndFlush(study).getId();

        entityManager.clear();

        Study found = studyRepository.findById(studyId)
                .orElseThrow();


        //then
        assertThat(found.getMethod()).isEqualTo(StudyMethod.OFFLINE);
        assertThat(found.getRegion()).isEqualTo("서울");
    }

    // Enum 값들이 String으로 저장되는지 테스트
    @Test
    void storesEnumValuesAsStrings(){

        // given
        Study study = createOnlineStudy(member);

        Long studyId = studyRepository.saveAndFlush(study).getId();

        //when
        String method = jdbcTemplate.queryForObject(
                "SELECT method FROM study WHERE study_id = ?",
                String.class,
                studyId
        );

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM study WHERE study_id = ?",
                String.class,
                studyId
        );

        //then
        assertThat(method).isEqualTo("ONLINE");
        assertThat(status).isEqualTo("OPEN");
    }

    // Member 지연로딩 테스트
    @Test
    void loadsMemberLazily(){

        // given
        Study study = createOnlineStudy(member);

        Long studyId = studyRepository.saveAndFlush(study).getId();
        String nickname = member.getNickname();

        entityManager.clear();

        //when
        Study found = studyRepository.findById(studyId)
                .orElseThrow();

        //then
        // Member가 아직 로딩되지 않았는지 확인
        assertThat(Hibernate.isInitialized(found.getMember())).isFalse();

        // 회원 필드에 접근
        assertThat(found.getMember().getNickname()).isEqualTo(nickname);

        // Member 초기화 확인
        assertThat(Hibernate.isInitialized(found.getMember())).isTrue();

    }

    // 특정 회원 ID로 스터디 목록 조회 테스트
    @Test
    void findsStudiesByMember(){

        // given
        Member another = createMember();

        Study first = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        );

        Study second = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        );

        studyRepository.saveAndFlush(
                createOnlineStudy(another)
        );

        Long memberId = member.getId();

        entityManager.clear();

        //when
        List<Study> studies = studyRepository.findByMember_IdAndDeletedAtIsNull(memberId);

        //then
        assertThat(studies)
                .extracting(Study::getId)
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId()
                );
    }

    // 삭제된 스터디는 조회에서 제외 테스트
    @Test
    void excludesDeletedStudy(){

        // given
        Study activeStudy = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        );

        Study deletedStudy = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        );

        Long deletedStudyId = deletedStudy.getId();
        Long memberId = member.getId();

        // 논리 삭제된 상태로 세팅
        jdbcTemplate.update(
                """
                    UPDATE study
                    SET deleted_at = CURRENT_TIMESTAMP(6)
                    WHERE study_id = ?
                    """,
                deletedStudyId
        );

        entityManager.clear();

        //when & then
        assertThat(
                studyRepository.findByIdAndDeletedAtIsNull(
                        deletedStudyId
                )
        ).isEmpty();

        // 일반적인 조회는 삭제된 테이터 조회 가능
        assertThat(
                studyRepository.findById(deletedStudyId)
        ).isPresent();

        // 개설자별 조회에서도 삭제된 데이터 제외
        assertThat(
                studyRepository.findByMember_IdAndDeletedAtIsNull(
                        memberId
                )
        )
                .extracting(Study::getId)
                .containsExactly(activeStudy.getId());
    }

    // 모집 인원에 관한 DB CHECK 제약 조건 작동 테스트
    @Test
    void databaseRejectsInvalidCapacity(){

        assertThatThrownBy(() ->
                insertStudyDirectly(member.getId(), 31)
        )
                .isInstanceOfSatisfying(
                        DataAccessException.class,
                        exception -> assertThat(
                                exception.getMostSpecificCause()
                        ).isInstanceOfSatisfying(
                                SQLException.class,
                                sqlException -> assertThat(
                                        sqlException.getErrorCode()
                                        // 3819 -> 	CHECK 제약조건 위반 코드
                                ).isEqualTo(3819)
                        )
                );
    }

    // 존재하지 않는 회원의 ID로 참조 차단 테스트
    @Test
    void databaseRejectsUnknownMember(){

        // 존재하지 않는 회원 ID 사용
        long unknownMemberId = Long.MAX_VALUE;

        assertThat(memberRepository.existsById(unknownMemberId)).isFalse();

        assertThatThrownBy(() ->
                insertStudyDirectly(unknownMemberId, 5)
        )
                .isInstanceOfSatisfying(
                        DataAccessException.class,
                        exception -> assertThat(
                                exception.getMostSpecificCause()
                        )
                                .isInstanceOfSatisfying(
                                        SQLException.class,
                                        sqlException -> assertThat(
                                                sqlException.getErrorCode()
                                                // 1452 -> 참조할 부모 데이터가 없는 FK 제약조건 위반 코드
                                        ).isEqualTo(1452)
                                )
                );
    }

    // fetch 조인을 통해 스터디 조회 시 연관 객체 member가 같이 조회되는지 테스트
    @Test
    void findsStudyWithMemberUsingFetchJoin(){

        // given
        Study study = createOnlineStudy(member);

        Long studyId = studyRepository.saveAndFlush(study).getId();

        String nickname = member.getNickname();

        entityManager.clear();

        //when
        Study found = studyRepository.findDetailById(studyId)
                .orElseThrow();

        //then
        assertThat(Hibernate.isInitialized(found.getMember())).isTrue();

        assertThat(found.getMember().getNickname()).isEqualTo(nickname);

    }

    // SQL 실행 횟수 비교 테스트
    @Test
    void fetchJoinReducesPreparedStatementCount(){

        Long studyId = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        ).getId();

        String nickname = member.getNickname();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);

        // Hibernate 내부에서 수집하는 통계 정보에 접근할 수 있는 객체
        Statistics statistics = sessionFactory.getStatistics();

        boolean previousSetting = statistics.isStatisticsEnabled();

        statistics.setStatisticsEnabled(true);

        try{

            entityManager.clear();
            statistics.clear();

            // 기존 조회 방식
            Study lazyStudy = studyRepository.findByIdAndDeletedAtIsNull(studyId).orElseThrow();

            assertThat(lazyStudy.getMember().getNickname()).isEqualTo(nickname);

            long lazyQueryCount = statistics.getPrepareStatementCount();

            entityManager.clear();
            statistics.clear();

            // fetch 조인 조회 방식
            Study fetchStudy = studyRepository.findDetailById(studyId).orElseThrow();

            assertThat(fetchStudy.getMember().getNickname())
                    .isEqualTo(nickname);

            long fetchJoinQueryCount = statistics.getPrepareStatementCount();

            // 비교 결과 출력
            System.out.println(
                    "LAZY 조회 SQL 수: " + lazyQueryCount
            );

            System.out.println(
                    "Fetch Join 조회 SQL 수: " + fetchJoinQueryCount
            );

            assertThat(lazyQueryCount).isEqualTo(2L);
            assertThat(fetchJoinQueryCount).isEqualTo(1L);
        }finally {

            // 다른 테스트에 영향을 주지 않도록 기존 설정 복원
            statistics.clear();
            statistics.setStatisticsEnabled(previousSetting);
        }
    }

    private Member createMember(){

        String loginId = "study_" + UUID.randomUUID()
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

    // 삭제된 스터디는 조회에서 제외 테스트
    @Test
    void detailQueryExcludesDeletedStudy(){

        // given
        Long studyId = studyRepository.saveAndFlush(
                createOnlineStudy(member)
        ).getId();

        jdbcTemplate.update(
                """
                    UPDATE study
                    SET deleted_at = CURRENT_TIMESTAMP(6)
                    WHERE study_id = ?
                    """,
                studyId
        );

        entityManager.clear();

        //when & then
        assertThat(studyRepository.findDetailById(studyId)).isEmpty();
    }

    private Study createOnlineStudy(Member owner){

        return Study.create(
                owner,
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                5
        );
    }

    // 객체 생성 시 검증 단계를 건너뛰기 위해 사용
    private void insertStudyDirectly(Long memberId, int capacity){

        jdbcTemplate.update(
                """
                    INSERT INTO study(
                        member_id,
                        title,
                        description,
                        method,
                        region,
                        capacity,
                        status
                    )
                    VALUES(?, ?, ?, 'ONLINE', NULL, ?, 'OPEN')
                    """,
                memberId,
                "스터디",
                "스터디 내용",
                capacity
        );
    }
}