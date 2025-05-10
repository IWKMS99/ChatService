package iwkms.chatapp.authservice.exception;

import iwkms.chatapp.authservice.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDto handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        logger.warn("User already exists: {}", ex.getMessage());
        return new ErrorResponseDto(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                getPathFromWebRequest(request)
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponseDto handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return new ErrorResponseDto(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                getPathFromWebRequest(request)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDto handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        return new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                getPathFromWebRequest(request)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (ex != null && ex.getBindingResult() != null) {
            ex.getBindingResult().getAllErrors().forEach(error -> {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error;
                    String fieldName = fieldError.getField();
                    String errorMessage = fieldError.getDefaultMessage();
                    if (fieldName != null && errorMessage != null) {
                        errors.put(fieldName, errorMessage);
                    } else {
                        logger.warn("Validation error with null field name or message for object: {} - Field: {}", fieldError.getObjectName(), fieldName);
                        errors.put(fieldName != null ? fieldName : "unknownField", errorMessage != null ? errorMessage : "Unknown error message for field " + (fieldName != null ? fieldName : "unknownField"));
                    }
                } else if (error != null) { // Handle ObjectError as well
                    String objectName = error.getObjectName();
                    String errorMessage = error.getDefaultMessage();
                    if (objectName != null && errorMessage != null) {
                        errors.put(objectName, errorMessage); // Using objectName as key for global errors
                    } else {
                        logger.warn("Global validation error with null object name or message. Code: {}", error.getCode());
                        errors.put(objectName != null ? objectName : "unknownObject", errorMessage != null ? errorMessage : "Unknown global error message for " + (objectName != null ? objectName : "unknownObject"));
                    }
                } else {
                    logger.warn("Encountered a null error object in BindingResult.");
                }
            });
        } else {
            logger.warn("MethodArgumentNotValidException or its BindingResult was null. Cannot extract specific field errors.");
            errors.put("generalValidation", "Invalid input data due to validation failure with missing details.");
        }

        logger.warn("Validation failed. Errors: {}", errors);

        return new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed: " + errors,
                getPathFromWebRequest(request)
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception caught by global handler: {}", ex.getMessage(), ex);

        return new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "Произошла внутренняя ошибка сервера.",
                getPathFromWebRequest(request)
        );
    }

    private String getPathFromWebRequest(WebRequest request) {
        String path = "unknown_path";
        if (request != null && request.getDescription(false) != null) {
            String description = request.getDescription(false);
            if (description.startsWith("uri=")) {
                path = description.substring(4);
            } else {
                path = description;
            }
        } else {
            logger.warn("WebRequest or its description was null. Path set to 'unknown_path'.");
        }
        return path;
    }
}