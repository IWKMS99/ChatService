package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.dto.WebSocketErrorDto;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private Validator validator;
    
    @Mock
    private SimpMessageHeaderAccessor headerAccessor;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ChatMessageSocketController controller;

    private ChatMessageDto messageDto;
    private ChatMessage chatMessage;
    
    private final String ROOM_ID = "test-room";
    private final String USER_NAME = "testUser";
    private final String MESSAGE_CONTENT = "Hello, world!";
    private final String SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() {
        messageDto = new ChatMessageDto();
        messageDto.setSenderUsername(USER_NAME);
        messageDto.setChatRoomId(ROOM_ID);
        messageDto.setContent(MESSAGE_CONTENT);

        chatMessage = new ChatMessage();
        chatMessage.setSenderUsername(USER_NAME);
        chatMessage.setChatRoomId(ROOM_ID);
        chatMessage.setContent(MESSAGE_CONTENT);
        chatMessage.setTimestamp(LocalDateTime.now());
        
        when(headerAccessor.getUser()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(USER_NAME);
        when(headerAccessor.getSessionId()).thenReturn(SESSION_ID);
    }

    @Test
    void sendMessage_Success() {
        when(chatService.saveMessage(any(ChatMessageDto.class))).thenReturn(chatMessage);
        
        controller.sendMessage(messageDto, headerAccessor);
        
        verify(chatService).saveMessage(any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/messages/" + ROOM_ID), any(ChatMessage.class));
    }

    @Test
    void sendMessage_OverrideUsername() {
        ArgumentCaptor<ChatMessageDto> dtoCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        when(chatService.saveMessage(dtoCaptor.capture())).thenReturn(chatMessage);
        
        messageDto.setSenderUsername("spoofedUser");
        
        controller.sendMessage(messageDto, headerAccessor);
        
        assertEquals(USER_NAME, dtoCaptor.getValue().getSenderUsername());
        verify(messagingTemplate).convertAndSend(eq("/topic/messages/" + ROOM_ID), any(ChatMessage.class));
    }

    @Test
    void sendMessage_UnauthorizedAccess() {
        when(chatService.saveMessage(any(ChatMessageDto.class)))
                .thenThrow(new UnauthorizedException("Вы не являетесь участником этой приватной комнаты"));
        
        controller.sendMessage(messageDto, headerAccessor);
        
        verify(chatService).saveMessage(any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq(SESSION_ID), 
            eq("/queue/errors"), 
            any(WebSocketErrorDto.class)
        );
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/messages/" + ROOM_ID), any(ChatMessage.class));
    }
    
    @Test
    void sendMessage_Unauthenticated() {
        when(headerAccessor.getUser()).thenReturn(null);
        
        controller.sendMessage(messageDto, headerAccessor);
        
        verify(chatService, never()).saveMessage(any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq(SESSION_ID), 
            eq("/queue/errors"), 
            any(WebSocketErrorDto.class)
        );
    }
} 