package iwkms.chatapp.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = jwtUtil.extractTokenFromRequest(request);

            if (jwt != null) {
                Authentication authentication = jwtUtil.getAuthentication(jwt);

                if (authentication instanceof AbstractAuthenticationToken authToken) {
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Set Authentication in SecurityContext for user '{}', roles '{}'", 
                                authentication.getName(), authentication.getAuthorities());
                } else if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.warn("Authentication object of type '{}' was set, but details could not be added.", 
                            authentication.getClass().getName());
                } else {
                    logger.debug("JWT Token is invalid or expired. No Authentication set.");
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication from JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
} 