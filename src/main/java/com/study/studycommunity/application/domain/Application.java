package com.study.studycommunity.application.domain;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.study.domain.Study;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "application",
        // 동일한 스터디에 중복 신청 방지 제약조건
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"study_id", "member_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "member_id",
            nullable = false,
            updatable = false
    )
    private Member member;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "study_id",
            nullable = false,
            updatable = false
    )
    private Study study;

    @Column(
            name = "message",
            nullable = false,
            length = 255
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(
            name = "status",
            nullable = false,
            length = 12
    )
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    private Application(
            Member member,
            Study study,
            String message
    ){

        // 검증 후 객체 값 설정
        validate(member, study, message);

        this.member = member;
        this.study = study;
        this.message = message;
        this.status = ApplicationStatus.PENDING;
    }

    public static Application create(
            Member member,
            Study study,
            String message
    ){

        return new Application(member, study, message);
    }

    private static void validate(
            Member member,
            Study study,
            String message
    ){

        if(member == null){
            throw new IllegalArgumentException(
                    "신청 회원은 필수입니다."
            );
        }

        if (study == null) {
            throw new IllegalArgumentException(
                    "신청 대상 스터디는 필수입니다."
            );
        }

        if (message == null
                || message.isBlank()
                || message.length() > 255) {

            throw new IllegalArgumentException(
                    "신청 메시지는 1~255자여야 합니다."
            );
        }

    }
}
