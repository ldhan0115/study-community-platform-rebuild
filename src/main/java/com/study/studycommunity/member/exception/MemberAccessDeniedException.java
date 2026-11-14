package com.study.studycommunity.member.exception;

// 로그인한 회원이 자신의 정보가 아닌 다른 회원의 정보를 수정하려고 할 때 사용
public class MemberAccessDeniedException extends RuntimeException{

    public MemberAccessDeniedException(String message){
        super(message);
    }
}
