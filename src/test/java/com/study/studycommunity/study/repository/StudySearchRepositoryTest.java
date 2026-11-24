package com.study.studycommunity.study.repository;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class StudySearchRepositoryTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    // EntityGraph 적용으로 인한 study 검색 시 member 객체 동시 로딩 테스트
    @Test
    void searchLoadsOwnersWithEntityGraph(){

        // given
        Member ownerA = createMember();
        Member ownerB = createMember();
        Member ownerC = createMember();

        createStudy(ownerA, "Spring 기초");
        createStudy(ownerB, "Spring 실전");
        createStudy(ownerC, "Java 기초");

        entityManager.clear();

        Specification<Study> spec = StudySpecifications.search(
                null, null, null, null
        );

        //when
        Page<Study> result = studyRepository.findAll(
                spec,
                PageRequest.of(0, 2)
        );

        //then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);

        // EntityGraph 적용 후 개설자가 초기화되어 있어야 함
        for(Study study : result.getContent()){
            assertThat(Hibernate.isInitialized(study.getMember())).isTrue();

            assertThat(study.getMember().getNickname()).isNotBlank();
        }
    }

    // EntityGraph 적용 시 추가적인 select문 감소 테스트
    @Test
    void entityGraphReducesAdditionalSelects(){

        Member ownerA = createMember();
        Member ownerB = createMember();
        Member ownerC = createMember();

        createStudy(ownerA, "스터디 A");
        createStudy(ownerA, "스터디 B");
        createStudy(ownerB, "스터디 C");
        createStudy(ownerB, "스터디 D");
        createStudy(ownerC, "스터디 E");

        entityManager.flush();
        entityManager.clear();

        Specification<Study> spec = StudySpecifications.search(
                null, null, null, null
        );

        PageRequest pageable = PageRequest.of(
                0, 4, Sort.by(Sort.Order.desc("id"))
        );

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);

        Statistics statistics = sessionFactory.getStatistics();
        boolean previousSetting = statistics.isStatisticsEnabled();

        statistics.setStatisticsEnabled(true);

        try{
            statistics.clear();

            // EntityGraph가 적용되지 않은 조회
            // 별도의 countSpec을 받는 오버로드 사용
            Page<Study> baseline = studyRepository.findAll(
                    spec,
                    spec,
                    pageable
            );

            for(Study study : baseline.getContent()){
                study.getMember().getNickname();
            }

            long baselineStatements = statistics.getPrepareStatementCount();

            entityManager.clear();
            statistics.clear();

            // EntityGraph를 적용한 조회
            Page<Study> optimized = studyRepository.findAll(
                    spec,
                    pageable
            );

            for(Study study : optimized.getContent()){
                assertThat(Hibernate.isInitialized(study.getMember())).isTrue();

                study.getMember().getNickname();
            }

            long optimizedStatements = statistics.getPrepareStatementCount();

            System.out.println("기존 방식: " + baselineStatements);
            System.out.println("EntityGraph 방식: " + optimizedStatements);

            assertThat(optimizedStatements)
                    .isLessThan(baselineStatements);
        }finally {
            statistics.clear();
            statistics.setStatisticsEnabled(previousSetting);
        }

    }

    private void createStudy(Member owner, String title){

        studyRepository.saveAndFlush(
                Study.create(
                        owner,
                        title,
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );
    }

    private Member createMember(){

        String loginId = "search_" + UUID.randomUUID()
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
}
