package com.study.studycommunity.study.domain;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.study.exception.InvalidStudyException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;


import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "study")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            updatable = false
    )
    private Member member;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "method", nullable = false, length = 10)
    private StudyMethod method;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private StudyStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Study(Member member,
                 String title,
                 String description,
                 StudyMethod method,
                 String region,
                 int capacity
    ) {

        this.member = member;
        this.title = title;
        this.description = description;
        this.method = method;
        this.region = region;
        this.capacity = capacity;

        // 스터디 개설 시 모집 상태는 항상 OPEN
        this.status = StudyStatus.OPEN;
    }

    // 정적 팩토리 메서드 통해서 스터디 생성
    public static Study create(
            Member member,
            String title,
            String description,
            StudyMethod method,
            String region,
            int capacity
    ){

        // 입력 값 검증
        validate(
                member,
                title,
                description,
                method,
                region,
                capacity
        );

        return new Study(
                member,
                title,
                description,
                method,
                region,
                capacity
        );
    }

    // dirty checking을 통한 수정
    public void updateDetails(
            String title,
            String description,
            StudyMethod method,
            String region,
            int capacity,
            StudyStatus status
    ){

        // 기존 검증 재사용
        validate(
                this.member,
                title,
                description,
                method,
                region,
                capacity
        );

        // 수정 시 모집 상태도 필수
        if(status == null){
            throw new InvalidStudyException(
                    "status",
                    "모집 상태는 필수입니다."
            );
        }

        // 검증 통과 후 상태 변경
        this.title = title;
        this.description = description;
        this.method = method;
        this.region = region;
        this.capacity = capacity;
        this.status = status;

    }

    // 스터디 삭제 (소프트 딜리트)
    public void softDelete(){

        if(this.deletedAt != null){
            return;
        }

        this.deletedAt = LocalDateTime.now();
    }

    private static void validate(
            Member member,
            String title,
            String description,
            StudyMethod method,
            String region,
            int capacity
    ){

        if(member == null){
            throw new InvalidStudyException(
                    "memerId",
                    "스터디 개설자는 필수입니다."
            );
        }

        if(title == null || title.isBlank() || title.length() > 100){

            throw new InvalidStudyException(
                    "title",
                    "스터디 제목은 1~100자여야 합니다."
            );
        }

        if(description == null || description.isBlank()
                || description.length() > 10000){
            throw new InvalidStudyException(
                    "description",
                    "스터디 내용은 1~10000자여야 합니다."
            );
        }

        if(method == null){
            throw new InvalidStudyException(
                    "method",
                    "스터디 진행 방식은 필수입니다."
            );
        }

        if(capacity < 1 || capacity > 30){
            throw new InvalidStudyException(
                    "capacity",
                    "모집 인원은 1~30명이어야 합니다."
            );
        }

        if(method == StudyMethod.ONLINE && region != null){
            throw new InvalidStudyException(
                    "region",
                    "온라인 스터디는 지역을 지정할 수 없습니다."
            );
        }

        if(method == StudyMethod.OFFLINE &&
                (region == null || region.isBlank() || region.length() > 100)){

            throw new InvalidStudyException(
                    "region",
                    "오프라인 스터디는 1~100자의 지역 정보가 필요합니다."
            );


        }

    }
}
