package com.study.studycommunity.global.exception;

import com.study.studycommunity.member.exception.DuplicateMemberException;
import com.study.studycommunity.member.exception.InvalidMemberPasswordException;
import com.study.studycommunity.member.exception.MemberAccessDeniedException;
import com.study.studycommunity.study.exception.InvalidStudyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bean Validation 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException exception
    ){

        FieldError fieldError = exception.getBindingResult().getFieldError();

        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : "잘못된 요청입니다.";

        String field = fieldError != null
                ? fieldError.getField()
                : null;

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_INPUT",
                message,
                field
        );
    }

    // 회원 정보 중복 처리
    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateMember(
            DuplicateMemberException exception
    ){
        return createErrorResponse(
                HttpStatus.CONFLICT,
                "MEMBER_DUPLICATE",
                exception.getMessage(),
                exception.getField()
        );
    }

    // 비밀번호 정책 위반 처리
    @ExceptionHandler(InvalidMemberPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(
            InvalidMemberPasswordException exception
    ){

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PASSWORD",
                exception.getMessage(),
                exception.getField()
        );
    }

    // 다른 회원의 정보에 접근하려할 때 차단 처리
    @ExceptionHandler(MemberAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleMemberAccessDenied(
            MemberAccessDeniedException exception
    ){
        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                "MEMBER_ACCESS_DENIED",
                exception.getMessage(),
                null
        );
    }

    // 스터디 개설 입력 값 검증 예외 처리
    @ExceptionHandler(InvalidStudyException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStudy(
            InvalidStudyException exception
    ){

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_STUDY",
                exception.getMessage(),
                exception.getField()
        );
    }

    // JSON 변환 오류 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleInvalidJson(
            HttpMessageNotReadableException exception
    ){

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                "요청 본문의 형식 또는 값이 올바르지 않습니다.",
                null
        );
    }

    // HTTP 응답 객체 생성하는 중복 로직
    private ResponseEntity<ProblemDetail> createErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String field
    ){

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);

        problem.setTitle("Request failed");
        problem.setProperty("code", code);

        if(field != null){
            problem.setProperty("field", field);
        }

        return ResponseEntity
                .status(status)
                .body(problem);
    }

}
