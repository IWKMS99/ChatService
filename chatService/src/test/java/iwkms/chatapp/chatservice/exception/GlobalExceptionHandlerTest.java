package iwkms.chatapp.chatservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final WebRequest webRequest = mock(WebRequest.class);

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("test-request");
    }

    @Test
    void handleResourceNotFoundException() {
        String errorMessage = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorDetails> response = 
            exceptionHandler.handleResourceNotFoundException(exception, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedException() {
        String errorMessage = "Unauthorized access";
        UnauthorizedException exception = new UnauthorizedException(errorMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorDetails> response = 
            exceptionHandler.handleUnauthorizedException(exception, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleAuthenticationException() {
        String errorMessage = "Authentication failed";
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn(errorMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorDetails> response = 
            exceptionHandler.handleAuthenticationException(exception, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Ошибка аутентификации: " + errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleAccessDeniedException() {
        String errorMessage = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorDetails> response = 
            exceptionHandler.handleAccessDeniedException(exception, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Доступ запрещен: " + errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleValidationExceptions() {
        String fieldName = "username";
        String errorMessage = "Username is required";
        
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("test", fieldName, errorMessage);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        ResponseEntity<Object> response = exceptionHandler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof GlobalExceptionHandler.ErrorDetails);
        GlobalExceptionHandler.ErrorDetails errorDetails = (GlobalExceptionHandler.ErrorDetails) response.getBody();
        assertEquals("Ошибка валидации", errorDetails.getMessage());
        assertTrue(errorDetails.getDetails().contains(fieldName));
        assertTrue(errorDetails.getDetails().contains(errorMessage));
    }

    @Test
    void handleAllExceptions() {
        String errorMessage = "Something went wrong";
        Exception exception = new Exception(errorMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorDetails> response = 
            exceptionHandler.handleAllExceptions(exception, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Произошла ошибка: " + errorMessage, response.getBody().getMessage());
    }
} 