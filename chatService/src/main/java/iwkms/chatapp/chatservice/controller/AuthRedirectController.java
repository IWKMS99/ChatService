package iwkms.chatapp.chatservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AuthRedirectController {
    @Value("${auth-service.url:http://localhost:8080/auth}")
    private String authServiceBaseUrl;

    @Value("${chat-service.url:http://localhost:8081/chat}")
    private String chatServiceRedirectUrl;

    @GetMapping("/login")
    public RedirectView redirectToLogin() {
        String redirectUrl = UriComponentsBuilder.fromHttpUrl(authServiceBaseUrl)
                .path("/login")
                .queryParam("redirect", chatServiceRedirectUrl)
                .toUriString();
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/register")
    public RedirectView redirectToRegister() {
        String redirectUrl = UriComponentsBuilder.fromHttpUrl(authServiceBaseUrl)
                .path("/register")
                .queryParam("redirect", chatServiceRedirectUrl)
                .toUriString();
        return new RedirectView(redirectUrl);
    }
} 