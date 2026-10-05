package com.fptu.swp391.sportscentermanager.exception;

import com.fptu.swp391.sportscentermanager.dto.ErrorResponseDTO;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponseDTO> handleAppException(AppException ex){
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(errorCode.getHttpStatus().value())
            .error(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();
            
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }
}
