package com.brightminds.rebuild.common.exception;

import com.brightminds.rebuild.common.response.ApiResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleValidation(
            WebExchangeBindException exception) {
        List<FieldViolation> violations = exception.getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        return ResponseEntity.status(errorCode.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(
                        errorCode.code(),
                        errorCode.message(),
                        new ValidationErrorData(violations)));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(ServerWebInputException exception) {
        ErrorCode errorCode = ErrorCode.MALFORMED_REQUEST;
        return ResponseEntity.status(errorCode.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unhandled exception", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null));
    }
}
