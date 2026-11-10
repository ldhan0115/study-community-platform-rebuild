package com.study.studycommunity.member.exception;

// 비밀번호의 UTF-8 바이트 길이가 제한을 초과했을 때 사용
public class InvalidMemberPasswordException extends RuntimeException{

    public InvalidMemberPasswordException(String message){
        super(message);
    }
}
