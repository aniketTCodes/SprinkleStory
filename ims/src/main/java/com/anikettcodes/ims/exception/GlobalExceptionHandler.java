package com.anikettcodes.ims.exception;

import com.anikettcodes.ims.dto.exception.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnexpectedException.class)
    public ResponseEntity<ExceptionResponse> handleUnexpectedException(HttpServletRequest req, UnexpectedException ex) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return new ResponseEntity<>(
                new ExceptionResponse("An unexpected error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(DataAlreadyExistException.class)
    public ResponseEntity<ExceptionResponse> handleDataAlreadyExistException(HttpServletRequest req, DataAlreadyExistException ex) {
        return new ResponseEntity<>(
                new ExceptionResponse(ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DataInUseException.class)
    public ResponseEntity<ExceptionResponse> handleDataInUseException(HttpServletRequest req, DataInUseException ex) {
        return new ResponseEntity<>(
                new ExceptionResponse(ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolation(HttpServletRequest req, DataIntegrityViolationException ex) {
        log.warn("Data integrity violation on {}", req.getRequestURI(), ex);
        return new ResponseEntity<>(
                new ExceptionResponse("Request conflicts with existing data"),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ExceptionResponse> handleOptimisticLock(HttpServletRequest req, ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock failure on {}", req.getRequestURI(), ex);
        return new ResponseEntity<>(
                new ExceptionResponse("Record was updated by someone else. Refresh and try again"),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DataDoesNotExist.class)
    public ResponseEntity<ExceptionResponse> handleDataDoesNotExist(HttpServletRequest req, DataDoesNotExist ex) {
        return new ResponseEntity<>(
                new ExceptionResponse(ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ExceptionResponse> handleInvalidData(HttpServletRequest req, InvalidDataException ex) {
        return new ResponseEntity<>(
                new ExceptionResponse(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }
}
