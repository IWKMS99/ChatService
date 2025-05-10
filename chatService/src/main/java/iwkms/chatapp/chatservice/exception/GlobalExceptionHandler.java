package iwkms.chatapp.chatservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDetails> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        return createErrorResponse(ex.getMessage(), request, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return createErrorResponse(ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        return createErrorResponse("Ошибка аутентификации: " + ex.getMessage(), 
                request, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        return createErrorResponse("Доступ запрещен: " + ex.getMessage(), 
                request, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "Ошибка валидации",
                errors.toString()
        );
        
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleAllExceptions(Exception ex, WebRequest request) {
        return createErrorResponse("Произошла ошибка: " + ex.getMessage(), 
                request, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private ResponseEntity<ErrorDetails> createErrorResponse(
            String message, WebRequest request, HttpStatus status) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                message,
                request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, status);
    }
    
    // Вложенный класс для представления деталей ошибки
    public static class ErrorDetails {
        private LocalDateTime timestamp;
        private String message;
        private String details;

        public ErrorDetails(LocalDateTime timestamp, String message, String details) {
            this.timestamp = timestamp;
            this.message = message;
            this.details = details;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getDetails() {
            return details;
        }
    }
} 