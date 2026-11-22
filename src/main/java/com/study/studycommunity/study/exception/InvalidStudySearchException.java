package com.study.studycommunity.study.exception;

// 잘못된 검색 요청 예외 -> 페이지 번호, 크기
public class InvalidStudySearchException extends RuntimeException{

    private final String field;

    public InvalidStudySearchException(
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
