package iwkms.chatapp.authservice.service;

import iwkms.chatapp.authservice.dto.RegistrationDto;
import iwkms.chatapp.authservice.exception.UserAlreadyExistsException;
import iwkms.chatapp.authservice.model.UserEntity;
import iwkms.chatapp.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String DEFAULT_USER_ROLE = "USER"; // Consider "ROLE_USER" if your security config expects prefixes

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity registerUser(RegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Пользователь с таким именем уже существует: " + registrationDto.getUsername());
        }
        UserEntity user = UserEntity.builder()
                .username(registrationDto.getUsername())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                // createdAt is now set automatically by @CreationTimestamp
                // enabled and accountNonLocked default to true in UserEntity
                .build();
        user.getRoles().add(DEFAULT_USER_ROLE);
        return userRepository.save(user);
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}