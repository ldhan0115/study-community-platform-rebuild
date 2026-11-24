package com.study.studycommunity.application.domain;


import com.study.studycommunity.member.domain.Member;
import com.study.studycommunity.study.domain.Study;
import com.study.studycommunity.study.domain.StudyMethod;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class ApplicationTest {

    private Member owner;
    private Member applicant;
    private Study study;

    @BeforeEach
    void setUp(){

        owner = Member.create(
                "owner01",
                "password",
                "owner01@test.com",
                "owner01"
        );

        applicant = Member.create(
                "applicant01",
                "password",
                "applicant01@test.com",
                "applicant01"
        );

        study = Study.create(
                owner,
                "Spring 백엔드 스터디",
                "Spring을 함께 공부해요",
                StudyMethod.ONLINE,
                null,
                5
        );
    }

    // 신청 생성 성공 테스트
    @Test
    void createsPendingApplication() {

        //when
        Application application = Application.create(
                applicant,
                study,
                "함께 공부하고 싶습니다."
        );

        //then
        assertThat(application.getMember())
                .isSameAs(applicant);

        assertThat(application.getStudy())
                .isSameAs(study);

        assertThat(application.getMessage())
                .isEqualTo("함께 공부하고 싶습니다.");

        assertThat(application.getStatus())
                .isEqualTo(ApplicationStatus.PENDING);


        // DB에 저장하기 전 상태 확인
        assertThat(application.getId()).isNull();
        assertThat(application.getCreatedAt()).isNull();
        assertThat(application.getUpdatedAt()).isNull();
    }

    // 신청 회원 누락 시 실패 테스트 (경계값 테스트)
    @Test
    void rejectsNullMember(){

        assertThatThrownBy(() ->
                Application.create(
                        null,
                        study,
                        "참가 신청합니다."
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("신청 회원은 필수입니다.");

    }

    // 신청 대상 스터디 누락 시 실패 테스트
    @Test
    void rejectsNullStudy(){

        assertThatThrownBy(() ->
                Application.create(
                        applicant,
                        null,
                        "참가 신청합니다."
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("신청 대상 스터디는 필수입니다.");
    }

    // 신청 메시지 공백 시 실패 테스트
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void rejectsBlankMessage(String message){

        assertThatThrownBy(() ->
                Application.create(
                        applicant,
                        study,
                        message
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("신청 메시지는 1~255자여야 합니다.");
    }

    // 신청 메시지 길이 255자 초과 시 실패 테스트
    @Test
    void rejectsMessageExceeding255Characters(){

        assertThatThrownBy(() ->
                Application.create(
                        applicant,
                        study,
                        "a".repeat(256)
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 신청 메시지 길이 조건 충족 시 성공 테스트
    @Test
    void acceptsMessageWithExactly255Characters() {

        Application application = Application.create(
                applicant,
                study,
                "a".repeat(255)
        );

        assertThat(application.getMessage()).hasSize(255);
        assertThat(application.getStatus())
                .isEqualTo(ApplicationStatus.PENDING);
    }
}
