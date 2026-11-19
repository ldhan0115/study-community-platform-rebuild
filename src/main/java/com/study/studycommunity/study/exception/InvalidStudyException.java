package com.study.studycommunity.study.exception;

// 스터디 개설 입력 값에 대한 예외
public class InvalidStudyException extends IllegalArgumentException{

    private final String field;

    public InvalidStudyException(
            String field,
            String message
    ){
        super(message);
        this.field = field;
    }

    public String getField(){
        return field;
    }
}
