package com.study.studycommunity.member.repository;

import com.study.studycommunity.member.domain.Member;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 회원 저장 및 조회 테스트
    @Test
    void saveAndFindMember(){
        // given
        Member member = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        //when
        Member savedMember = memberRepository.saveAndFlush(member);
        Long savedId = savedMember.getId();

        // 영속성 컨텍스트를 비워 DB에서 다시 조회
        em.clear();

        Member foundMember = memberRepository.findById(savedId)
                .orElseThrow();

        //then
        assertThat(foundMember.getId()).isEqualTo(savedId);
        assertThat(foundMember.getLoginId()).isEqualTo("member");
        assertThat(foundMember.getEmail()).isEqualTo("member@test.com");
        assertThat(foundMember.getNickname()).isEqualTo("멤버");

        // 평문 비밀번호가 아닌 해시값 저장 확인
        assertThat(foundMember.getPasswordHash()).isNotEqualTo("password1234");

        assertThat(foundMember.getCreatedAt()).isNotNull();
        assertThat(foundMember.getUpdatedAt()).isNotNull();
    }

    // 로그인 아이디 조회 테스트
    @Test
    void findByLoginId(){
        // given
        Member member = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        memberRepository.saveAndFlush(member);

        //when
        Member foundMember = memberRepository.findByLoginId("member")
                .orElseThrow();

        //then
        assertThat(foundMember.getLoginId()).isEqualTo("member");
    }

    // 회원 존재 여부 테스트
    @Test
    void checkMemberExistence(){
        // given
        Member member = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        memberRepository.saveAndFlush(member);

        //when & then
        assertThat(memberRepository.existsByLoginId("member")).isTrue();
        assertThat(memberRepository.existsByEmail("member@test.com")).isTrue();
        assertThat(memberRepository.existsByNickname("멤버")).isTrue();

        assertThat(memberRepository.existsByLoginId("unknown")).isFalse();
    }


    // 로그인 아이디 중복 테스트
    @Test
    void rejectDuplicateLoginId(){
        // given
        memberRepository.saveAndFlush(
                createMember(
                        "member",
                        "member@test.com",
                        "멤버"
                )
        );

        Member duplicate = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        //when & then
        Assertions.assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 이메일 중복 테스트
    @Test
    void rejectDuplicateEmail() {
        // given
        memberRepository.saveAndFlush(
                createMember(
                        "member",
                        "member@test.com",
                        "멤버"
                )
        );

        Member duplicate = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 닉네임 중복 테스트
    @Test
    void rejectDuplicateNickname() {
        // given
        memberRepository.saveAndFlush(
                createMember(
                        "member",
                        "member@test.com",
                        "멤버"
                )
        );

        Member duplicate = createMember(
                "member",
                "member@test.com",
                "멤버"
        );

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Member createMember(
            String loginId,
            String email,
            String nickname
    ){

        String encodedPassword = passwordEncoder.encode("password1234");

        return Member.create(
                loginId,
                encodedPassword,
                email,
                nickname
        );
    }
}