package com.study.studycommunity.study.exception;

// 권한 없는 스터디에 접근해 수정/삭제할 시 사용
public class StudyAccessDeniedException extends RuntimeException{

    public StudyAccessDeniedException(){
        super("해당 스터디를 수정할 권한이 없습니다.");
    }

    // 작업에 맞는 메시지 지정하도록 추가
    public StudyAccessDeniedException(String message){
        super(message);
    }
}
