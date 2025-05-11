package iwkms.chatapp.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.common.security.config.JwtSecurityConfig;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends JwtSecurityConfig {

    public SecurityConfig(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(jwtUtil, objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", 
                                 "/error",
                                 "/css/**", "/js/**", "/images/**",
                                 "/webjars/**",
                                 "/login", "/register",
                                 "/chat",
                                 "/profile",
                                 "/ws/**"
                ).permitAll()
                .requestMatchers("/api/v1/messages/**", "/api/v1/rooms/**").authenticated()
                .anyRequest().authenticated()
        );
        
        return configureSecurityFilterChain(http);
    }
} 