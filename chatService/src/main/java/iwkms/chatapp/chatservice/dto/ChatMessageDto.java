package iwkms.chatapp.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessageDto {
    @NotBlank(message = "Sender username cannot be blank")
    private String senderUsername;

    @NotBlank(message = "Chat room ID cannot be blank")
    private String chatRoomId;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    public ChatMessageDto(String senderUsername, String chatRoomId, String content) {
        this.senderUsername = senderUsername;
        this.chatRoomId = chatRoomId;
        this.content = content;
    }
}