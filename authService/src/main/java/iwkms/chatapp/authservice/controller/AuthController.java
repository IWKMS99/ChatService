package iwkms.chatapp.authservice.controller;

import iwkms.chatapp.authservice.dto.*;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.service.UserService;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<GenericResponseDto> register(@Valid @RequestBody RegistrationDto registrationDto) {
        UserEntity savedUser = userService.registerUser(registrationDto); // Can throw UserAlreadyExistsException
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponseDto("Пользователь " + savedUser.getUsername() + " успешно зарегистрирован"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDto(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Неверные учетные данные", request.getRequestURI()));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto(HttpStatus.FORBIDDEN.value(), "Forbidden", "Учетная запись пользователя отключена: " + e.getMessage(), request.getRequestURI()));
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto(HttpStatus.FORBIDDEN.value(), "Forbidden", "Учетная запись пользователя заблокирована: " + e.getMessage(), request.getRequestURI()));
        } catch (AuthenticationException e) { // Catchall for other auth issues
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDto(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Ошибка аутентификации: " + e.getMessage(), request.getRequestURI()));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(new AuthResponseDto(jwt, userDetails.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUserInfo(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDto(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Вы не авторизованы или токен не предоставлен/недействителен", request.getRequestURI()));
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString(); // Fallback
        }

        return ResponseEntity.ok(new UserInfoResponseDto(username, authentication.getAuthorities()));
    }
}