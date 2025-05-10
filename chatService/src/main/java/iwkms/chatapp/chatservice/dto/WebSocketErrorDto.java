package iwkms.chatapp.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketErrorDto {
    private LocalDateTime timestamp;
    private String errorType;
    private List<String> messages;
    private String originalDestination;

    public WebSocketErrorDto(String errorType, List<String> messages, String originalDestination) {
        this.timestamp = LocalDateTime.now();
        this.errorType = errorType;
        this.messages = messages;
        this.originalDestination = originalDestination;
    }
}