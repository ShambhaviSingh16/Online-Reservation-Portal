package com.trainticket.api;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages="com.trainticket.api")
public class ApiErrors {
    @ExceptionHandler(com.trainticket.dao.AdvancedReservationDAO.Conflict.class) ResponseEntity<?> reservation(RuntimeException e) {
        return ResponseEntity.status(409).body(Map.of("error",e.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed":e.getReason()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,
        HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,IllegalArgumentException.class})
    ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("error","Invalid request fields")); }
    @ExceptionHandler({DataAccessException.class,java.sql.SQLException.class}) ResponseEntity<?> database(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Database request failed",e);
        return ResponseEntity.status(409).body(Map.of("error","Request conflicts with existing records or database is unavailable"));
    }
}
