package iwkms.chatapp.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private String username;
    private Collection<?> authorities; // Using Collection<?> to match Authentication.getAuthorities()
} 