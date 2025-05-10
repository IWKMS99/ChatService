package iwkms.chatapp.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatRoomDto {
    
    @NotBlank(message = "ID комнаты не должен быть пустым")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "ID комнаты может содержать только буквы, цифры, дефисы и подчеркивания")
    @Size(min = 3, max = 50, message = "ID комнаты должен содержать от 3 до 50 символов")
    private String roomId;
    
    @NotBlank(message = "Название комнаты не должно быть пустым")
    @Size(min = 3, max = 100, message = "Название комнаты должно содержать от 3 до 100 символов")
    private String name;
    
    @Size(max = 255, message = "Описание должно содержать не более 255 символов")
    private String description;
    
    private boolean isPrivate;
} 