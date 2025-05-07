package iwkms.chatapp.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;

    public AuthResponseDto(String accessToken, String username) {
        this.accessToken = accessToken;
        this.username = username;
    }
}