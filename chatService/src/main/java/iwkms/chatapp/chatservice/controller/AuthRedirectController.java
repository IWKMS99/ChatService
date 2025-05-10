package iwkms.chatapp.chatservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AuthRedirectController {

    // URL сервиса аутентификации
    private static final String AUTH_SERVICE_URL = "http://localhost:8082"; 

    @GetMapping("/api/v1/auth/login")
    public RedirectView redirectToLogin() {
        // Перенаправление на страницу входа в сервисе аутентификации
        return new RedirectView(AUTH_SERVICE_URL + "/login");
    }

    @GetMapping("/api/v1/auth/register")
    public RedirectView redirectToRegister() {
        // Перенаправление на страницу регистрации в сервисе аутентификации
        return new RedirectView(AUTH_SERVICE_URL + "/register");
    }
} 