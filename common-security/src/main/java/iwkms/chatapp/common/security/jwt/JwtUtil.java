package iwkms.chatapp.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final Key key;
    private final long jwtExpirationMs;
    private final String jwtHeader;
    private final String jwtPrefix;
    public static final String ROLES_CLAIM = "roles";

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration.ms:3600000}") long jwtExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtHeader = "Authorization";
        this.jwtPrefix = "Bearer ";
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtHeader);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtPrefix)) {
            String token = bearerToken.substring(jwtPrefix.length());
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public Authentication getAuthentication(String token) {
        if (!validateToken(token)) {
            return null;
        }
        
        String username = getUsernameFromToken(token);
        Claims claims = getClaimsFromToken(token);
        String rolesString = claims.get(ROLES_CLAIM, String.class);

        List<SimpleGrantedAuthority> authorities;
        if (StringUtils.hasText(rolesString)) {
            authorities = Arrays.stream(rolesString.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = Collections.emptyList();
            logger.warn("Roles claim missing or empty in JWT for user {}. Using empty authorities.", username);
        }

        UserDetails userDetails = User.builder()
                .username(username)
                .password("")
                .authorities(authorities)
                .build();
        
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public String generateToken(Authentication authentication) {
        String username;
        Collection<? extends GrantedAuthority> authorities;

        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            authorities = userDetails.getAuthorities();
        } else {
            username = authentication.getPrincipal().toString();
            authorities = authentication.getAuthorities();
            logger.warn("Authentication principal is not UserDetails. Username: {}, Authorities: {}", 
                    username, authorities);
        }

        String roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim(ROLES_CLAIM, roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
} 