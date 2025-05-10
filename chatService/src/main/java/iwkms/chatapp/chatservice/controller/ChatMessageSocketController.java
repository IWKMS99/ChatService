package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.dto.WebSocketErrorDto;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.stream.Collectors;

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
    public void sendMessage(@Payload ChatMessageDto chatMessageDto, 
                           SimpMessageHeaderAccessor headerAccessor) {
        // Проверка валидации
        Errors errors = new BeanPropertyBindingResult(chatMessageDto, "chatMessageDto");
        validator.validate(chatMessageDto, errors);

        if (errors.hasErrors()) {
            sendValidationErrors(chatMessageDto, headerAccessor, errors);
            return;
        }

        try {
            // Получаем текущего пользователя из контекста безопасности
            Authentication authentication = (Authentication) headerAccessor.getUser();
            if (authentication == null) {
                sendError(headerAccessor, "Unauthorized", "Вы не авторизованы");
                return;
            }
            
            String username = authentication.getName();
            
            // Убедиться, что отправитель сообщения - текущий пользователь
            if (!username.equals(chatMessageDto.getSenderUsername())) {
                chatMessageDto.setSenderUsername(username);
            }
            
            // Сохраняем сообщение
            ChatMessage savedMessage = chatService.saveMessage(chatMessageDto);
            
            // Отправляем сообщение всем подписчикам
            String destination = "/topic/messages/" + savedMessage.getChatRoomId();
            messagingTemplate.convertAndSend(destination, savedMessage);
            
        } catch (UnauthorizedException e) {
            sendError(headerAccessor, "Access Denied", e.getMessage());
        } catch (Exception e) {
            sendError(headerAccessor, "Error", "Произошла ошибка: " + e.getMessage());
        }
    }
    
    private void sendValidationErrors(ChatMessageDto chatMessageDto, 
                                     SimpMessageHeaderAccessor headerAccessor, 
                                     Errors errors) {
        List<String> errorMessages = errors.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        WebSocketErrorDto errorDto = new WebSocketErrorDto(
                "Validation Error",
                errorMessages,
                headerAccessor.getDestination()
        );

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorDto);
        }
    }
    
    private void sendError(SimpMessageHeaderAccessor headerAccessor, 
                          String errorType, 
                          String errorMessage) {
        WebSocketErrorDto errorDto = new WebSocketErrorDto(
                errorType,
                List.of(errorMessage),
                headerAccessor.getDestination()
        );

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorDto);
        }
    }
}