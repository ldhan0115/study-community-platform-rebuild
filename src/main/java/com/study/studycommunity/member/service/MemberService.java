package com.study.studycommunity.member.service;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.dto.MemberSignupRequest;
import com.study.studycommunity.member.dto.MemberSignupResponse;
import com.study.studycommunity.member.exception.DuplicateMemberException;
import com.study.studycommunity.member.exception.InvalidMemberPasswordException;
import com.study.studycommunity.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request){

        // 로그인 아이디 중복 확인
        if(memberRepository.existsByLoginId(request.loginId())){
            throw new DuplicateMemberException("loginId", "이미 사용 중인 로그인 아이디입니다.");
        }

        // 이메일 중복 확인
        if(memberRepository.existsByEmail(request.email())){
            throw new DuplicateMemberException("email", "이미 사용 중인 이메일입니다.");
        }

        // 닉네임 중복 확인
        if(memberRepository.existsByNickname(request.nickname())){
            throw new DuplicateMemberException("nickname", "이미 사용 중인 닉네임입니다.");
        }

        // 비밀번호 바이트 길이 확인
        int passwordBytes = request.password().getBytes(StandardCharsets.UTF_8).length;

        if(passwordBytes > 72){
            throw new InvalidMemberPasswordException("비밀번호는 UTF-8 기준 72바이트를 초과할 수 없습니다.");
        }

        // 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.password());

        // 회원 Entity 생성
        Member member = Member.create(
                request.loginId(),
                encodedPassword,
                request.email(),
                request.nickname()
        );

        // 회원 저장
        try{
            Member savedMember = memberRepository.saveAndFlush(member);

            // 응답 DTO 반환
            return MemberSignupResponse.from(savedMember);

        }catch(DataIntegrityViolationException e){

            // 동시 요청 등으로 DB UNIQUE 제약조건을 위반한 경우
            throw new DuplicateMemberException("member", "이미 사용 중인 회원 정보가 존재합니다.");
        }
    }

}
