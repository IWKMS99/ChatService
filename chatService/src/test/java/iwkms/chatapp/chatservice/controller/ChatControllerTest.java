package iwkms.chatapp.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import iwkms.chatapp.chatservice.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;
    
    @MockBean
    private ChatMessageRepository chatMessageRepository;
    
    @MockBean
    private ChatRoomRepository chatRoomRepository;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ChatMessageDto messageDto;
    private ChatMessage message;
    private final String ROOM_ID = "test-room";
    private final String USER_NAME = "testUser";
    private final String MESSAGE_CONTENT = "Hello, world!";

    @BeforeEach
    void setUp() {
        messageDto = new ChatMessageDto(USER_NAME, ROOM_ID, MESSAGE_CONTENT);

        message = new ChatMessage();
        message.setId(1L);
        message.setSenderUsername(USER_NAME);
        message.setChatRoomId(ROOM_ID);
        message.setContent(MESSAGE_CONTENT);
        message.setTimestamp(LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "testUser")
    void sendMessage_Success() throws Exception {
        when(chatService.saveMessage(any(ChatMessageDto.class))).thenReturn(message);

        mockMvc.perform(post("/api/v1/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderUsername", is(USER_NAME)))
                .andExpect(jsonPath("$.content", is(MESSAGE_CONTENT)));

        verify(chatService).saveMessage(any(ChatMessageDto.class));
    }

    @Test
    @WithMockUser(username = "differentUser")
    void sendMessage_WithDifferentUsername_OverridesUsername() throws Exception {
        String authenticatedUser = "differentUser";
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setSenderUsername(authenticatedUser);
        savedMessage.setChatRoomId(ROOM_ID);
        savedMessage.setContent(MESSAGE_CONTENT);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(chatService.saveMessage(any(ChatMessageDto.class))).thenReturn(savedMessage);

        mockMvc.perform(post("/api/v1/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderUsername", is(authenticatedUser)));

        verify(chatService).saveMessage(argThat(dto -> authenticatedUser.equals(dto.getSenderUsername())));
    }

    @Test
    @WithMockUser(username = "testUser")
    void sendMessage_Unauthorized() throws Exception {
        when(chatService.saveMessage(any(ChatMessageDto.class)))
                .thenThrow(new UnauthorizedException("Вы не являетесь участником этой приватной комнаты"));

        mockMvc.perform(post("/api/v1/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isForbidden());

        verify(chatService).saveMessage(any(ChatMessageDto.class));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMessagesByRoom_Success() throws Exception {
        List<ChatMessage> messages = Arrays.asList(message);
        when(chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME)).thenReturn(messages);

        mockMvc.perform(get("/api/v1/messages/{chatRoomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].senderUsername", is(USER_NAME)))
                .andExpect(jsonPath("$[0].content", is(MESSAGE_CONTENT)));

        verify(chatService).getMessagesByChatRoom(ROOM_ID, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMessagesByRoom_Unauthorized() throws Exception {
        when(chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME))
                .thenThrow(new UnauthorizedException("Вы не имеете доступа к этой приватной комнате"));

        mockMvc.perform(get("/api/v1/messages/{chatRoomId}", ROOM_ID))
                .andExpect(status().isForbidden());

        verify(chatService).getMessagesByChatRoom(ROOM_ID, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMessagesByRoom_EmptyList() throws Exception {
        when(chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/messages/{chatRoomId}", ROOM_ID))
                .andExpect(status().isNoContent());

        verify(chatService).getMessagesByChatRoom(ROOM_ID, USER_NAME);
    }
} 