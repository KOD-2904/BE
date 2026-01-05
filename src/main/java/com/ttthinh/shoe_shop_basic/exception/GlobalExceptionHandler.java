package com.ttthinh.shoe_shop_basic.exception;

import com.ttthinh.shoe_shop_basic.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(value = RuntimeException.class)
//    ResponseEntity<ApiResponse> runtimeExceptionHandler(RuntimeException e) {
//        ErrorCode errorCode = e.get
//    }
//    @ExceptionHandler(value = Exception.class)
//    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception){
//        ApiResponse apiResponse = new ApiResponse();
//
//        apiResponse.setCode(ErrorCode.UNAUTHENTICATED_EXCEPTION.getCode());
//        apiResponse.setMessage(ErrorCode.UNAUTHENTICATED_EXCEPTION.getMessage());
//        UsernamePasswordAuthenticationToken authToken =
//                new UsernamePasswordAuthenticationToken(username, password);
//        authenticationManager.authenticate(authToken);
//        return ResponseEntity.badRequest().body(apiResponse);
//    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingException(Exception e) {
        e.printStackTrace(); // Log lỗi để debug

        ApiResponse apiResponse = new ApiResponse();

        // Nếu là AccessDeniedException từ Security, xử lý riêng
        if (e instanceof AccessDeniedException) {
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED_ACCESS;
            apiResponse.setCode(errorCode.getCode());
            apiResponse.setMessage(errorCode.getMessage());
            return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
        }

        // Nếu là AuthenticationException
        if (e instanceof AuthenticationException) {
            ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
            apiResponse.setCode(errorCode.getCode());
            apiResponse.setMessage(errorCode.getMessage());
            return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
        }

        // Các exception khác
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED_EXCEPTION;
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
    }
//    @ExceptionHandler(value = AccessDeniedException.class)
//    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException e) {
//        ApiResponse apiResponse = new ApiResponse();
//        ErrorCode errorCode = ErrorCode.UNAUTHORIZED_ACCESS;
//
//        apiResponse.setCode(errorCode.getCode());  // Mã lỗi tự định nghĩa (1008)
//        apiResponse.setMessage(errorCode.getMessage());
//
//        // Dùng httpStatusCode từ ErrorCode, KHÔNG dùng getCode()
//        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
//    }
//
//    // Xử lý AuthenticationException (401 - Unauthorized)
//    @ExceptionHandler(value = {AuthenticationException.class, BadCredentialsException.class})
//    ResponseEntity<ApiResponse> handlingAuthenticationException(AuthenticationException e) {
//        ApiResponse apiResponse = new ApiResponse();
//        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
//
//        apiResponse.setCode(errorCode.getCode());  // Mã lỗi tự định nghĩa (1009)
//        apiResponse.setMessage(errorCode.getMessage());
//
//        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
//    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handelingAppException(AppException e) {
        ApiResponse apiResponse = new ApiResponse();
        ErrorCode errorCode = e.getErrorCode();
        apiResponse.setMessage(errorCode.getMessage());
        apiResponse.setCode(errorCode.getCode());
        return ResponseEntity.badRequest().body(apiResponse);
    }
//    @ExceptionHandler(value = MethodArgumentNotValidException.class)
//    ResponseEntity<ApiResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException e) {
//        ApiResponse apiResponse = new ApiResponse();
//        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
//
//        apiResponse.setCode(errorCode.getCode());
//        apiResponse.setMessage(errorCode.getMessage());
//
//        // Thêm chi tiết lỗi validation
//        Map<String, String> errors = new HashMap<>();
//        e.getBindingResult().getFieldErrors().forEach(error ->
//                errors.put(error.getField(), error.getDefaultMessage())
//        );
//
//        if (apiResponse.getResult() == null) {
//            apiResponse.setResult(new HashMap<>());
//        }
//        ((Map<String, Object>) apiResponse.getResult()).put("validationErrors", errors);
//
//        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
//    }
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handelingMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(ErrorCode.VALIDATION_ERROR.getCode());
        apiResponse.setMessage(ErrorCode.VALIDATION_ERROR.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
