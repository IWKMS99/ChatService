package iwkms.chatapp.authservice.controller;

import iwkms.chatapp.authservice.config.jwt.JwtUtil;
import iwkms.chatapp.authservice.dto.AuthResponseDto;
import iwkms.chatapp.authservice.dto.LoginRequestDto;
import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationDto registrationDto) {
        UserEntity savedUser = userService.registerUser(registrationDto);
        return ResponseEntity.ok("Пользователь " + savedUser.getUsername() + " успешно зарегистрирован");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Неверные учетные данные");
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body("Учетная запись пользователя отключена: " + e.getMessage());
        } catch (LockedException e) {
            return ResponseEntity.status(403).body("Учетная запись пользователя заблокирована: " + e.getMessage());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Ошибка аутентификации: " + e.getMessage());
        }


        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(new AuthResponseDto(jwt, userDetails.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("Вы не авторизованы или токен не предоставлен/недействителен");
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString(); // Fallback
        }

        return ResponseEntity.ok("Текущий пользователь: " + username + ", Роли: " + authentication.getAuthorities());
    }
}