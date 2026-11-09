package com.study.studycommunity.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Member() {
    }

    private Member(
            String loginId,
            String passwordHash,
            String email,
            String nickname) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.nickname = nickname;
    }

    // 정적 팩토리 메서드 통해서 회원 생성
    public static Member create(
            String loginId,
            String passwordHash,
            String email,
            String nickname
    ){
        return new Member(
                loginId,
                passwordHash,
                email,
                nickname);
    }

}
