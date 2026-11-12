package com.study.studycommunity.member.security;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// Spring Security가 로그인할 사용자의 정보를 가져올 때 사용하는 인터페이스
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId)
            throws UsernameNotFoundException {

        // 사용자가 입력한 로그인 ID로 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("존재하지 않는 회원입니다."));

        // Member 엔티티를 Spring Security의 UserDetails로 변환
        return User.withUsername(member.getLoginId())
                .password(member.getPasswordHash())
                .roles("USER")
                .build();
    }
}
