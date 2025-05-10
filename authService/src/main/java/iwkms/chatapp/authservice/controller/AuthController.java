package iwkms.chatapp.authservice.controller;

import iwkms.chatapp.authservice.dto.LoginRequestDto;
import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.dto.ErrorResponseDto;
import iwkms.chatapp.authservice.dto.GenericResponseDto;
import iwkms.chatapp.authservice.dto.AuthResponseDto;
import iwkms.chatapp.authservice.dto.UserInfoResponseDto;
import iwkms.chatapp.authservice.exception.UserAlreadyExistsException;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.service.UserService;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDto registrationDto) {
        logger.info("Attempting to register user: {}", registrationDto.getUsername());
        try {
            UserEntity savedUser = userService.registerUser(registrationDto);
            if (savedUser == null) {
                logger.error("User registration failed unexpectedly for username: {}", registrationDto.getUsername());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDto(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Registration Error",
                        "User registration failed unexpectedly after service call.",
                        "/api/v1/auth/register"
                ));
            }
            logger.info("User {} registered successfully", savedUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(new GenericResponseDto("Пользователь " + savedUser.getUsername() + " успешно зарегистрирован"));
        } catch (UserAlreadyExistsException e) {
            logger.warn("Registration failed for user {}: {}", registrationDto.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during registration for user {}: {}", registrationDto.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDto(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred during registration.",
                    "/api/v1/auth/register"
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequestDto, HttpServletRequest request) {
        logger.info("Attempting to login user: {}", loginRequestDto.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword())
            );

            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Authentication failed or returned null for user: {}. isAuthenticated: {}", 
                            loginRequestDto.getUsername(), authentication != null ? authentication.isAuthenticated() : "null_auth_object");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "Authentication failed or authentication object is invalid.",
                        "/api/v1/auth/login"
                ));
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            logger.info("User {} logged in successfully", username);
            return ResponseEntity.ok(new AuthResponseDto(jwt, username));
        } catch (AuthenticationException e) {
            logger.warn("Login failed for user {}: {}", loginRequestDto.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login for user {}: {}", loginRequestDto.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDto(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred during login.",
                    "/api/v1/auth/login"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDto> getCurrentUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Attempt to access /me without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserInfoResponseDto userInfo = new UserInfoResponseDto(
                userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
        );
        logger.info("Fetched current user info for: {}", userDetails.getUsername());
        return ResponseEntity.ok(userInfo);
    }
}