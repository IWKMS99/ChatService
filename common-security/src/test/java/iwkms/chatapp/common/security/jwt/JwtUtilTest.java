package iwkms.chatapp.common.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtUtil jwtUtilShortExpiry;
    private JwtUtil jwtUtilDifferentSecret;

    private final String testSecret = "testSecretKeyForJwtUtilTestingPurposesLongEnough";
    private final long testExpirationMs = 3600000;
    private final long shortExpirationMs = 1;
    private final String differentSecret = "anotherSecretKeyForTestingDifferentSignatureValue";

    @Mock
    private Authentication authenticationMock;

    @Mock
    private HttpServletRequest requestMock;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(testSecret, testExpirationMs);
        jwtUtilShortExpiry = new JwtUtil(testSecret, shortExpirationMs);
        jwtUtilDifferentSecret = new JwtUtil(differentSecret, testExpirationMs);
    }

    private UserDetails createUserDetails(String username, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new User(username, "", authorities);
    }

    @Test
    void testGenerateToken_Success() {
        UserDetails userDetails = createUserDetails("testUser", List.of("ROLE_USER", "ROLE_ADMIN"));
        when(authenticationMock.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtil.generateToken(authenticationMock);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtUtil.getClaimsFromToken(token);
        assertNotNull(claims);
        assertEquals("testUser", claims.getSubject());
        String rolesClaim = claims.get(JwtUtil.ROLES_CLAIM, String.class);
        assertNotNull(rolesClaim);
        Set<String> actualRoles = Set.of(rolesClaim.split(","));
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), actualRoles);
        assertTrue(claims.getExpiration().after(new Date()));
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    void testValidateToken_ValidToken() {
        UserDetails userDetails = createUserDetails("validUser", List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtil.generateToken(auth);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        UserDetails userDetails = createUserDetails("userForSigTest", List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtil.generateToken(auth); 
        assertFalse(jwtUtilDifferentSecret.validateToken(token));
    }

    @Test
    void testValidateToken_ExpiredToken() throws InterruptedException {
        UserDetails userDetails = createUserDetails("userForExpiryTest", List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtilShortExpiry.generateToken(auth);
        Thread.sleep(shortExpirationMs + 100);
        assertFalse(jwtUtilShortExpiry.validateToken(token));
    }

    @Test
    void testValidateToken_MalformedToken() {
        assertFalse(jwtUtil.validateToken("this.is.not.a.jwt"));
    }
    
    @Test
    void testValidateToken_EmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testGetUsernameFromToken_Success() {
        UserDetails userDetails = createUserDetails("expectedUser", List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtil.generateToken(auth);
        assertEquals("expectedUser", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void testGetAuthentication_ValidToken() {
        UserDetails userDetails = createUserDetails("authUser", List.of("ROLE_MODERATOR"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtil.generateToken(auth);

        Authentication resultAuth = jwtUtil.getAuthentication(token);
        assertNotNull(resultAuth);
        assertTrue(resultAuth.isAuthenticated());
        assertEquals("authUser", resultAuth.getName());
        assertTrue(resultAuth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MODERATOR")));
    }
    
    @Test
    void testGetAuthentication_ValidToken_NoRoles() {
        UserDetails userDetails = createUserDetails("authUserNoRoles", List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtUtil.generateToken(auth);

        Authentication resultAuth = jwtUtil.getAuthentication(token);
        assertNotNull(resultAuth);
        assertTrue(resultAuth.isAuthenticated());
        assertEquals("authUserNoRoles", resultAuth.getName());
        assertTrue(resultAuth.getAuthorities().isEmpty());
    }

    @Test
    void testGetAuthentication_InvalidToken() {
        UserDetails userDetails = createUserDetails("expiredUser", List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String expiredToken = jwtUtilShortExpiry.generateToken(auth);
        try {
            Thread.sleep(shortExpirationMs + 100); 
        } catch (InterruptedException e) { 
            Thread.currentThread().interrupt(); 
        }
        assertNull(jwtUtil.getAuthentication(expiredToken)); 
    }

    @Test
    void testExtractTokenFromRequest_ValidBearerToken() {
        String tokenValue = "sampleTokenValue";
        when(requestMock.getHeader("Authorization")).thenReturn("Bearer " + tokenValue);
        assertEquals(tokenValue, jwtUtil.extractTokenFromRequest(requestMock));
    }

    @Test
    void testExtractTokenFromRequest_MissingHeader() {
        when(requestMock.getHeader("Authorization")).thenReturn(null);
        assertNull(jwtUtil.extractTokenFromRequest(requestMock));
    }

    @Test
    void testExtractTokenFromRequest_InvalidPrefix() {
        when(requestMock.getHeader("Authorization")).thenReturn("Basic somevalue");
        assertNull(jwtUtil.extractTokenFromRequest(requestMock));
    }

    @Test
    void testExtractTokenFromRequest_NoTokenAfterPrefix() {
        when(requestMock.getHeader("Authorization")).thenReturn("Bearer ");
        assertNull(jwtUtil.extractTokenFromRequest(requestMock));
    }
    
    @Test
    void testExtractTokenFromRequest_TokenIsEmptyString() {
        when(requestMock.getHeader("Authorization")).thenReturn("Bearer ");
        assertNull(jwtUtil.extractTokenFromRequest(requestMock)); 
    }
} 