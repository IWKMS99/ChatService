package iwkms.chatapp.authservice.controller;

import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationDto registrationDto) {
        UserEntity savedUser = userService.registerUser(registrationDto);
        return ResponseEntity.ok("Пользователь " + savedUser.getUsername() + " успешно зарегистрирован");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username,
                                        @RequestParam String password) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, password);
        try {
            Authentication auth = authenticationManager.authenticate(authRequest);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return ResponseEntity.ok("Вы успешно вошли, " + username);
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body("Неверные учетные данные");
        } catch (DisabledException | LockedException e) {
            return ResponseEntity.badRequest().body("Вход невозможен: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<String> currentUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Вы не авторизованы");
        }
        return ResponseEntity.ok("Текущий пользователь: " + authentication.getName());
    }
}