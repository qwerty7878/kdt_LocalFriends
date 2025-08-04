package com.backend.kdt.auth.common;

import com.backend.kdt.auth.entity.MsgEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<MsgEntity> globalException(Exception e) {
        return ResponseEntity.badRequest()
                .body(new MsgEntity(e.getMessage(), ""));
    }
}