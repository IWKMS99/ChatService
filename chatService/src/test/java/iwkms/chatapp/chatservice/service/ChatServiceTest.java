package iwkms.chatapp.chatservice.service;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatService chatService;

    private ChatMessageDto messageDto;
    private ChatMessage chatMessage;
    private ChatRoom chatRoom;
    private final String ROOM_ID = "test-room";
    private final String USER_NAME = "testUser";
    private final String MESSAGE_CONTENT = "Hello, world!";

    @BeforeEach
    void setUp() {
        messageDto = new ChatMessageDto(USER_NAME, ROOM_ID, MESSAGE_CONTENT);

        chatMessage = new ChatMessage();
        chatMessage.setId(1L);
        chatMessage.setSenderUsername(USER_NAME);
        chatMessage.setChatRoomId(ROOM_ID);
        chatMessage.setContent(MESSAGE_CONTENT);
        chatMessage.setTimestamp(LocalDateTime.now());

        chatRoom = new ChatRoom(ROOM_ID, "Test Room", "Test Description", false, USER_NAME);
    }

    @Test
    void saveMessage_ExistingRoom_Success() {
        when(chatRoomRepository.existsByRoomId(ROOM_ID)).thenReturn(true);
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        assertNotNull(savedMessage);
        assertEquals(USER_NAME, savedMessage.getSenderUsername());
        assertEquals(ROOM_ID, savedMessage.getChatRoomId());
        assertEquals(MESSAGE_CONTENT, savedMessage.getContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void saveMessage_NonExistingRoom_CreatesRoom() {
        when(chatRoomRepository.existsByRoomId(ROOM_ID)).thenReturn(false);
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(true);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        assertNotNull(savedMessage);
        assertEquals(USER_NAME, savedMessage.getSenderUsername());
        assertEquals(ROOM_ID, savedMessage.getChatRoomId());
        assertEquals(MESSAGE_CONTENT, savedMessage.getContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void saveMessage_NoAccess_ThrowsException() {
        when(chatRoomRepository.existsByRoomId(ROOM_ID)).thenReturn(true);
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> 
            chatService.saveMessage(messageDto));
            
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void getMessagesByChatRoom_HasAccess_Success() {
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(true);
        when(chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(messages);

        List<ChatMessage> result = chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(USER_NAME, result.get(0).getSenderUsername());
        assertEquals(MESSAGE_CONTENT, result.get(0).getContent());
    }

    @Test
    void getMessagesByChatRoom_NoAccess_ThrowsException() {
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> 
            chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME));
            
        verify(chatMessageRepository, never()).findByChatRoomIdOrderByTimestampAsc(anyString());
    }

    @Test
    void getMessagesByChatRoom_EmptyList_Success() {
        when(chatRoomService.checkMembership(ROOM_ID, USER_NAME)).thenReturn(true);
        when(chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(new ArrayList<>());

        List<ChatMessage> result = chatService.getMessagesByChatRoom(ROOM_ID, USER_NAME);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
} 