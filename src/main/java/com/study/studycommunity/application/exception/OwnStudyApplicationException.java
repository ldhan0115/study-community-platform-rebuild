package com.study.studycommunity.application.exception;

// 자신의 스터디에 신청할 경우 사용
public class OwnStudyApplicationException extends RuntimeException{

    public OwnStudyApplicationException(){
        super("자신이 개설한 스터디에는 참가 신청할 수 없습니다.");
    }
}
