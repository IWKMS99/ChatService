package iwkms.chatapp.chatservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AuthRedirectController {
    @Value("${auth-service.url:http://localhost:8082}")
    private String authServiceBaseUrl;

    @Value("${chat-service.redirect-url:http://localhost:8080/chat}")
    private String chatServiceRedirectUrl;

    @GetMapping("/login")
    public RedirectView redirectToLogin() {
        String baseUrl = authServiceBaseUrl.endsWith("/") ? authServiceBaseUrl.substring(0, authServiceBaseUrl.length() -1) : authServiceBaseUrl;
        String path = "/login";

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("redirect", chatServiceRedirectUrl)
                .toUriString();
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/register")
    public RedirectView redirectToRegister() {
        String baseUrl = authServiceBaseUrl.endsWith("/") ? authServiceBaseUrl.substring(0, authServiceBaseUrl.length() -1) : authServiceBaseUrl;
        String path = "/register";

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("redirect", chatServiceRedirectUrl)
                .toUriString();
        return new RedirectView(redirectUrl);
    }
}