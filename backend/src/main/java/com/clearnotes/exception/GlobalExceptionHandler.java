package com.clearnotes.exception;

import com.clearnotes.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> emailConflict(EmailAlreadyRegisteredException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "An account with this email already exists.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception,
                                                          HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.", request);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> invalidPassword(InvalidPasswordException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", exception.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> badCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(issue -> issue.getField() + " " + issue.getDefaultMessage()).orElse("Request validation failed.");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(InvalidAudioFileException.class)
    public ResponseEntity<ApiErrorResponse> invalidAudio(InvalidAudioFileException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", exception.getMessage(), request);
    }

    @ExceptionHandler({UploadTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiErrorResponse> uploadTooLarge(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "Audio file exceeds the 100 MB upload limit.", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> missingUpload(MissingServletRequestPartException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_FILE", "Include an audio file in the file field.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedExportFormatException.class)
    public ResponseEntity<ApiErrorResponse> unsupportedExport(UnsupportedExportFormatException exception,
                                                                HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_EXPORT_FORMAT", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to access this resource.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception for {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request);
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), code, message, request.getRequestURI()));
    }
}
