package iwkms.chatapp.chatservice.service;

import iwkms.chatapp.chatservice.dto.ChatRoomDto;
import iwkms.chatapp.chatservice.exception.ResourceNotFoundException;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private ChatRoomDto chatRoomDto;
    private ChatRoom chatRoom;
    private final String ROOM_ID = "test-room";
    private final String USER_NAME = "testUser";
    private final String OWNER_NAME = "ownerUser";

    @BeforeEach
    void setUp() {
        chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomId(ROOM_ID);
        chatRoomDto.setName("Test Room");
        chatRoomDto.setDescription("Test Description");
        chatRoomDto.setPrivate(false);

        chatRoom = new ChatRoom(ROOM_ID, "Test Room", "Test Description", false, OWNER_NAME);
        chatRoom.addMember(USER_NAME);
    }

    @Test
    void createChatRoom_Success() {
        when(chatRoomRepository.existsByRoomId(anyString())).thenReturn(false);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        ChatRoom result = chatRoomService.createChatRoom(chatRoomDto, OWNER_NAME);

        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(OWNER_NAME, result.getOwnerUsername());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void createChatRoom_AlreadyExists() {
        when(chatRoomRepository.existsByRoomId(ROOM_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            chatRoomService.createChatRoom(chatRoomDto, OWNER_NAME));
            
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void getChatRoomById_Success() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        ChatRoom result = chatRoomService.getChatRoomById(ROOM_ID);

        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
    }

    @Test
    void getChatRoomById_NotFound() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            chatRoomService.getChatRoomById(ROOM_ID));
    }

    @Test
    void addMemberToChatRoom_Success() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        String newMember = "newUser";
        chatRoomService.addMemberToChatRoom(ROOM_ID, newMember, USER_NAME);

        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void addMemberToChatRoom_Unauthorized() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        String newMember = "newUser";
        String unauthorizedUser = "unauthorizedUser";
        
        assertThrows(UnauthorizedException.class, () -> 
            chatRoomService.addMemberToChatRoom(ROOM_ID, newMember, unauthorizedUser));
            
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void removeMemberFromChatRoom_ByOwner_Success() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        chatRoomService.removeMemberFromChatRoom(ROOM_ID, USER_NAME, OWNER_NAME);

        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void removeMemberFromChatRoom_SelfRemove_Success() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        chatRoomService.removeMemberFromChatRoom(ROOM_ID, USER_NAME, USER_NAME);

        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void removeMemberFromChatRoom_Unauthorized() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        String unauthorizedUser = "otherUser";
        chatRoom.addMember(unauthorizedUser);
        
        assertThrows(UnauthorizedException.class, () -> 
            chatRoomService.removeMemberFromChatRoom(ROOM_ID, USER_NAME, unauthorizedUser));
            
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void removeMemberFromChatRoom_CannotRemoveOwner() {
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        String regularUser = "regularUser";
        chatRoom.addMember(regularUser);
        
        assertThrows(UnauthorizedException.class, () -> 
            chatRoomService.removeMemberFromChatRoom(ROOM_ID, OWNER_NAME, regularUser));
            
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void checkMembership_PublicRoom_Success() {
        chatRoom.setPrivate(false);
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        String nonMember = "nonMember";
        boolean result = chatRoomService.checkMembership(ROOM_ID, nonMember);

        assertTrue(result);
    }

    @Test
    void checkMembership_PrivateRoom_Member_Success() {
        chatRoom.setPrivate(true);
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        boolean result = chatRoomService.checkMembership(ROOM_ID, USER_NAME);

        assertTrue(result);
    }

    @Test
    void checkMembership_PrivateRoom_NonMember_Failure() {
        chatRoom.setPrivate(true);
        when(chatRoomRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        String nonMember = "nonMember";
        boolean result = chatRoomService.checkMembership(ROOM_ID, nonMember);

        assertFalse(result);
    }

    @Test
    void getPublicChatRooms_Success() {
        List<ChatRoom> publicRooms = Arrays.asList(chatRoom);
        when(chatRoomRepository.findByIsPrivateFalse()).thenReturn(publicRooms);

        List<ChatRoom> result = chatRoomService.getPublicChatRooms();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ROOM_ID, result.get(0).getRoomId());
    }

    @Test
    void getUserChatRooms_Success() {
        List<ChatRoom> userRooms = Arrays.asList(chatRoom);
        when(chatRoomRepository.findByMembersContains(USER_NAME)).thenReturn(userRooms);

        List<ChatRoom> result = chatRoomService.getUserChatRooms(USER_NAME);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ROOM_ID, result.get(0).getRoomId());
    }
} 