package com.study.studycommunity.study.exception;

public class StudyAccessDeniedException extends RuntimeException{

    public StudyAccessDeniedException(){
        super("해당 스터디를 수정할 권한이 없습니다.");
    }
}
