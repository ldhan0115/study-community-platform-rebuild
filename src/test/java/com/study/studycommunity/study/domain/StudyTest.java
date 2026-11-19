package com.study.studycommunity.study.domain;

import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.study.exception.InvalidStudyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

// 스터디 객체 단위 테스트
class StudyTest {

    private final Member member = Member.create(
            "studyOwner",
            "password",
            "owner@test.com",
            "스터디 개설자"
    );

    // 온라인 스터디 생성 테스트
    @Test
    void createsOnlineStudy(){

        // given & when
        Study study = Study.create(
                member,
                "Spring 스터디",
                "Spring을 함께 공부해요",
                StudyMethod.ONLINE,
                null,
                5
        );

        //then
        assertThat(study.getMember()).isSameAs(member);
        assertThat(study.getTitle()).isEqualTo("Spring 스터디");
        assertThat(study.getMethod()).isEqualTo(StudyMethod.ONLINE);
        assertThat(study.getRegion()).isNull();
        assertThat(study.getCapacity()).isEqualTo(5);

        // 최초 모집 상태 확인
        assertThat(study.getStatus()).isEqualTo(StudyStatus.OPEN);

        // 아직 저장하지 않아서 ID 없음
        assertThat(study.getId()).isNull();
        assertThat(study.getDeletedAt()).isNull();
    }

    // 오프라인 스터디 생성 테스트
    @Test
    void createsOfflineStudy(){
        // given & when
        Study study = Study.create(
                member,
                "Java 스터디",
                "매주 모여서 Java를 공부해요.",
                StudyMethod.OFFLINE,
                "서울",
                5
        );

        //then
        assertThat(study.getMethod()).isEqualTo(StudyMethod.OFFLINE);
        assertThat(study.getRegion()).isEqualTo("서울");
        assertThat(study.getStatus()).isEqualTo(StudyStatus.OPEN);
    }

    // 스터디 생성 시 멤버 누락하면 실패 테스트
    @Test
    void rejectsMissingMember(){

        assertThatThrownBy(() ->
                Study.create(
                        null,
                        "Spring 스터디",
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 스터디 제목에 공백 입력시 실패 테스트
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsBlankTitle(String title){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        title,
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 스터디 제목에 100자 초과 입력 시 실패 테스트
    @Test
    void rejectsTitleExceeding100Characters(){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "a".repeat(101),
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 스터디 설명에 공백 시 실패 테스트
    @Test
    void rejectsBlankDescription(){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        " ",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 스터디 진행 방식 누락 시 실패 테스트
    @Test
    void rejectsMissingMethod(){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "스터디 내용",
                        null,
                        null,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 모집 인원 요건 미충족 시 실패 테스트
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 31, 100})
    void rejectsInvalidCapacity(int capacity){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        null,
                        capacity
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모집 인원은 1~30명이어야 합니다.");
    }

    // 모집 인원 요건 충족 시 성공 테스트 (경계 값 사용)
    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void acceptsCapacityBoundary(int capacity) {

        Study study = Study.create(
                member,
                "Spring 스터디",
                "스터디 내용",
                StudyMethod.ONLINE,
                null,
                capacity
        );

        assertThat(study.getCapacity()).isEqualTo(capacity);
    }

    // 온라인 스터디에 지역 입력 시 실패 테스트
    @Test
    void rejectsRegionForOnlineStudy() {

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "스터디 내용",
                        StudyMethod.ONLINE,
                        "서울",
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 오프라인 스터디에 지역 값 공백 시 실패 테스트
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsMissingRegionForOfflineStudy(String region) {

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "스터디 내용",
                        StudyMethod.OFFLINE,
                        region,
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 지역 값 100자 초과 시 실패 테스트
    @Test
    void rejectsRegionExceeding100Characters() {

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "스터디 내용",
                        StudyMethod.OFFLINE,
                        "a".repeat(101),
                        5
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // 스터디 내용 10000자 초과 시 실패 테스트
    @Test
    void rejectsDescriptionExceeding10000Characters(){

        assertThatThrownBy(() ->
                Study.create(
                        member,
                        "Spring 스터디",
                        "a".repeat(10001),
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        ).isInstanceOfSatisfying(
                InvalidStudyException.class,
                exception -> assertThat(exception.getField())
                        .isEqualTo("description")
        );
    }

    // 스터디 내용 10000자 이내로 작성 시 성공 테스트
    @Test
    void acceptsDescriptionWith10000Characters(){

        Study study = Study.create(
                member,
                "Spring 스터디",
                "a".repeat(10000),
                StudyMethod.ONLINE,
                null,
                5
        );

        assertThat(study.getDescription()).hasSize(10000);
    }

}