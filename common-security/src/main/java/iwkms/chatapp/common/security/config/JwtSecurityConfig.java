package iwkms.chatapp.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.common.security.jwt.JwtAuthenticationEntryPoint;
import iwkms.chatapp.common.security.jwt.JwtAuthenticationFilter;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public abstract class JwtSecurityConfig {

    protected final JwtAuthenticationFilter jwtAuthenticationFilter;
    protected final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    protected JwtSecurityConfig(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil);
        this.jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    protected SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
} 