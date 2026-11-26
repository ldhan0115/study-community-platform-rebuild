package com.study.studycommunity.application.exception;

// 잘못된 조회 조건 예외 처리
public class InvalidApplicationQueryException extends RuntimeException{

    public InvalidApplicationQueryException(String message){
        super(message);
    }
}
