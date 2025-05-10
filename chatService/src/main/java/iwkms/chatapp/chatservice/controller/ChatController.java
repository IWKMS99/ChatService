package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class ChatController {
    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatMessage> sendMessage(@Valid @RequestBody ChatMessageDto messageDto,
                                                  Authentication authentication) {
        // Убедиться, что отправитель сообщения - текущий пользователь
        String username = authentication.getName();
        if (!username.equals(messageDto.getSenderUsername())) {
            messageDto.setSenderUsername(username);
        }
        
        ChatMessage savedMessage = chatService.saveMessage(messageDto);
        return new ResponseEntity<>(savedMessage, HttpStatus.CREATED);
    }

    @GetMapping("/{chatRoomId}")
    public ResponseEntity<List<ChatMessage>> getMessagesByRoom(@PathVariable String chatRoomId,
                                                             Authentication authentication) {
        String username = authentication.getName();
        List<ChatMessage> messages = chatService.getMessagesByChatRoom(chatRoomId, username);
        if (messages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(messages);
    }
}