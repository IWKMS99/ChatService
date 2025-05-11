package iwkms.chatapp.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.common.security.config.JwtSecurityConfig;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends JwtSecurityConfig {

    public SecurityConfig(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(jwtUtil, objectMapper);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/register-process", 
                                 "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll() 
                .anyRequest().authenticated()
        );
        
        return configureSecurityFilterChain(http); 
    }
}