package com.study.studycommunity.study.exception;

// 존재하지 않는 스터디 조회 시 사용
public class StudyNotFoundException extends RuntimeException{

    public StudyNotFoundException(){
        super("스터디를 찾을 수 없습니다.");
    }
}
