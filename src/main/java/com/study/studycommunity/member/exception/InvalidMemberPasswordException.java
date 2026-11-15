package com.study.studycommunity.member.exception;

// 비밀번호의 UTF-8 바이트 길이가 제한을 초과했을 때 사용
public class InvalidMemberPasswordException extends RuntimeException{

    private final String field;

    // 기존 코드와의 호환성 유지
    public InvalidMemberPasswordException(String message){
        this("password" ,message);
    }

    // 오류 발생 필드 지정
    public InvalidMemberPasswordException(
            String field,
            String message
    ){
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
