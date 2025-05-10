package iwkms.chatapp.authservice.service;

import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.exception.UserAlreadyExistsException;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegistrationDto registrationDto;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        registrationDto = new RegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password123");

        userEntity = UserEntity.builder()
                .username("testuser")
                .password("encodedPassword")
                .build();
        userEntity.getRoles().add(UserService.DEFAULT_USER_ROLE);
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        UserEntity savedUser = userService.registerUser(registrationDto);

        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertTrue(savedUser.getRoles().contains(UserService.DEFAULT_USER_ROLE));

        ArgumentCaptor<UserEntity> userEntityArgumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userEntityArgumentCaptor.capture());
        UserEntity capturedUser = userEntityArgumentCaptor.getValue();

        assertEquals("testuser", capturedUser.getUsername());
        assertEquals("encodedPassword", capturedUser.getPassword());
        assertTrue(capturedUser.getRoles().contains(UserService.DEFAULT_USER_ROLE));

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto);
        });

        assertEquals("Пользователь с таким именем уже существует: testuser", exception.getMessage());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testFindByUsername_UserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userEntity));

        Optional<UserEntity> foundUserOptional = userService.findByUsername("testuser");

        assertTrue(foundUserOptional.isPresent());
        UserEntity foundUser = foundUserOptional.get();
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("encodedPassword", foundUser.getPassword());

        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testFindByUsername_UserDoesNotExist() {
        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        Optional<UserEntity> foundUserOptional = userService.findByUsername("nonexistentuser");

        assertFalse(foundUserOptional.isPresent());

        verify(userRepository, times(1)).findByUsername("nonexistentuser");
    }
} 