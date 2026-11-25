package com.study.studycommunity.application.exception;

// 모집 마감된 스터디에 신청한 경우
public class StudyNotOpenException extends RuntimeException{

    public StudyNotOpenException() {
        super("모집 중인 스터디에만 참가 신청할 수 있습니다.");
    }
}
