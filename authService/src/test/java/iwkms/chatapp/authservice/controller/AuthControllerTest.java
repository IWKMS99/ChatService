package iwkms.chatapp.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.authservice.dto.LoginRequestDto;
import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.exception.UserAlreadyExistsException;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.service.UserService;
import iwkms.chatapp.common.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private iwkms.chatapp.authservice.repository.UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private RegistrationDto registrationDto;
    private LoginRequestDto loginRequestDto;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        registrationDto = new RegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password123");
        // registrationDto.setEmail("test@example.com"); // Если бы было поле email

        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("testuser");
        loginRequestDto.setPassword("password123");

        userEntity = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .build();
        userEntity.getRoles().add("USER");
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        given(userService.registerUser(any(RegistrationDto.class))).willReturn(userEntity);

        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto))
                .with(csrf())); // Добавляем CSRF токен, если CSRF включен (WebMvcTest может его требовать по умолчанию)

        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Пользователь testuser успешно зарегистрирован"));
    }

    @Test
    void testRegisterUser_ValidationError_EmptyUsername() throws Exception {
        registrationDto.setUsername(""); // Невалидное имя

        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto))
                .with(csrf()));

        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }
    
    @Test
    void testRegisterUser_ValidationError_EmptyPassword() throws Exception {
        registrationDto.setPassword(""); 

        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto))
                .with(csrf()));

        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }

    @Test
    void testRegisterUser_UserAlreadyExists() throws Exception {
        String exceptionMessage = "Пользователь с таким именем уже существует: " + registrationDto.getUsername();
        given(userService.registerUser(any(RegistrationDto.class)))
                .willThrow(new UserAlreadyExistsException(exceptionMessage));

        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto))
                .with(csrf()));

        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(exceptionMessage))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }


    @Test
    void testLogin_Success() throws Exception {
        // Используем org.springframework.security.core.userdetails.User
        org.springframework.security.core.userdetails.UserDetails springUserDetails = 
            new org.springframework.security.core.userdetails.User(loginRequestDto.getUsername(), "encodedPassword", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication successfulAuth = new UsernamePasswordAuthenticationToken(springUserDetails, "password123", springUserDetails.getAuthorities());

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(successfulAuth);
        given(jwtUtil.generateToken(any(Authentication.class))).willReturn("test.jwt.token");

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(loginRequestDto.getUsername()));
    }

    @Test
    void testLogin_BadCredentials() throws Exception {
        String exceptionMessage = "Неверные учетные данные";
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException(exceptionMessage));

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));

        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(exceptionMessage))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }
    
    @Test
    void testLogin_UserDisabled() throws Exception {
        String exceptionMessage = "Учетная запись пользователя отключена";
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new DisabledException(exceptionMessage));

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));

        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(exceptionMessage))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void testLogin_UserLocked() throws Exception {
        String exceptionMessage = "Учетная запись пользователя заблокирована";
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new LockedException(exceptionMessage));

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));

        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(exceptionMessage))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }
    
    @Test
    void testLogin_AuthenticationException() throws Exception {
        String exceptionMessage = "Ошибка аутентификации";
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new AuthenticationServiceException(exceptionMessage));

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));
        
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(exceptionMessage))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }


    @Test
    void testLogin_ValidationError() throws Exception {
        loginRequestDto.setUsername(""); // Невалидные данные

        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto))
                .with(csrf()));

        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER", "ROLE_ADMIN"})
    void testCurrentUserInfo_Authenticated() throws Exception {
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me")
                .with(csrf())); // CSRF может быть не нужен для GET, но для консистентности

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }
    
    @Test
    void testCurrentUserInfo_Unauthorized_NoToken() throws Exception {
        // Выполняем запрос без какой-либо аутентификации
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me")
                .with(csrf()));

        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication token was either missing or invalid."))
                .andExpect(jsonPath("$.detail").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value(""));
    }
} 