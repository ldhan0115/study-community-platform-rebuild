package com.study.studycommunity.member.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.dto.MemberSignupRequest;
import com.study.studycommunity.member.dto.MemberSignupResponse;
import com.study.studycommunity.member.exception.DuplicateMemberException;
import com.study.studycommunity.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signupEncodesPassword(){
        // given
        MemberSignupRequest request = new MemberSignupRequest(
                "member",
                "password",
                "member@test.com",
                "멤버"
        );

        //when
        MemberSignupResponse response = memberService.signup(request);

        Member savedMember = memberRepository.findById(response.memberId())
                .orElseThrow();

        //then
        assertThat(savedMember.getLoginId()).isEqualTo("member");
        assertThat(savedMember.getPasswordHash()).isNotEqualTo("password");

        assertThat(passwordEncoder.matches(
                "password",
                savedMember.getPasswordHash()
        )).isTrue();
    }

    @Test
    void signupRejectsDuplicateLoginId(){
        // given
        MemberSignupRequest request1 = new MemberSignupRequest(
                "member",
                "password",
                "member@test.com",
                "멤버"
        );

        MemberSignupRequest request2 = new MemberSignupRequest(
                "member",
                "password",
                "member@test.com",
                "멤버"
        );

        memberService.signup(request1);

        //when & then
        assertThatThrownBy(() -> memberService.signup(request2))
                .isInstanceOf(DuplicateMemberException.class);
    }
}