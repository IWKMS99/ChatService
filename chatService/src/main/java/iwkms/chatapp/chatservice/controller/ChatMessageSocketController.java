package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.dto.WebSocketErrorDto;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.util.stream.Collectors;
import java.util.List;

@Controller
public class ChatMessageSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final Validator validator;

    @Autowired
    public ChatMessageSocketController(SimpMessagingTemplate messagingTemplate,
                                       ChatService chatService,
                                       Validator validator) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.validator = validator;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessageDto, SimpMessageHeaderAccessor headerAccessor) {
        Errors errors = new BeanPropertyBindingResult(chatMessageDto, "chatMessageDto");
        validator.validate(chatMessageDto, errors);

        if (errors.hasErrors()) {
            List<String> errorMessages = errors.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());

            WebSocketErrorDto errorDto = new WebSocketErrorDto(
                    "Validation Error",
                    errorMessages,
                    headerAccessor.getDestination()
            );

            // Получаем ID сессии пользователя, который отправил сообщение
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                // Отправляем ошибку на "личную" очередь пользователя
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorDto, headerAccessor.getMessageHeaders());
                System.err.println("Sent validation errors to user " + sessionId + ": " + errorMessages);
            } else {
                // Фоллбэк, если ID сессии не доступен (маловероятно для STOMP)
                System.err.println("Validation errors for WebSocket message, but no session ID found: " + errorMessages);
            }
            return;
        }

        ChatMessage savedMessage = chatService.saveMessage(chatMessageDto);
        String destination = "/topic/messages/" + savedMessage.getChatRoomId();
        messagingTemplate.convertAndSend(destination, savedMessage);
    }
}