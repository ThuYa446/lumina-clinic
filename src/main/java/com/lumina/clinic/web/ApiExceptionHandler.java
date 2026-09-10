package com.lumina.clinic.web;

import com.lumina.clinic.booking.DomainException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception) {
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(400, "VALIDATION_FAILED", "Check the highlighted fields and try again.");
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("fieldErrors", fields);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
            ConstraintViolationException.class})
    ProblemDetail malformed(Exception exception) {
        return problem(400, "INVALID_REQUEST", "A required value is missing or incorrectly formatted.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException exception) {
        // SQL and parameter values can contain personal data. Do not return or log them.
        return problem(409, "BOOKING_CONFLICT", "This request conflicts with an existing reservation. Refresh availability and try again.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException exception) {
        return problem(400, "INVALID_REQUEST", "A supplied value is invalid.");
    }

    private ProblemDetail problem(int status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), message);
        detail.setProperty("code", code);
        return detail;
    }
}
