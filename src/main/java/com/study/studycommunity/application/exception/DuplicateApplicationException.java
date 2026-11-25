package com.study.studycommunity.application.exception;

public class DuplicateApplicationException
        extends RuntimeException {

    public DuplicateApplicationException() {
        super("이미 참가 신청한 스터디입니다.");
    }
}