package com.study.studycommunity.member.exception;

import lombok.Getter;

@Getter
public class DuplicateMemberException extends RuntimeException{

    // 어떤 필드에서 중복이 발생했는지 구분하기 위해 사용
    private final String field;

    public DuplicateMemberException(String field, String message) {
        super(message);
        this.field = field;
    }
}
