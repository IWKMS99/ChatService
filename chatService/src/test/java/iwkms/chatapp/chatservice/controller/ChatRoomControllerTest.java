package iwkms.chatapp.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.chatservice.dto.ChatRoomDto;
import iwkms.chatapp.chatservice.exception.ResourceNotFoundException;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;
    
    @MockBean
    private ChatMessageRepository chatMessageRepository;
    
    @MockBean
    private ChatRoomRepository chatRoomRepository;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ChatRoomDto chatRoomDto;
    private ChatRoom chatRoom;
    private final String ROOM_ID = "test-room";
    private final String ROOM_NAME = "Test Room";
    private final String USERNAME = "testUser";

    @BeforeEach
    void setUp() {
        chatRoomDto = new ChatRoomDto();
        chatRoomDto.setName(ROOM_NAME);
        chatRoomDto.setDescription("Test description");
        chatRoomDto.setPrivate(false);

        chatRoom = new ChatRoom();
        chatRoom.setRoomId(ROOM_ID);
        chatRoom.setName(ROOM_NAME);
        chatRoom.setDescription("Test description");
        chatRoom.setPrivate(false);
        chatRoom.setOwnerUsername(USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void createChatRoom_Success() throws Exception {
        chatRoomDto.setRoomId("new-test-room");

        when(chatRoomService.createChatRoom(any(ChatRoomDto.class), eq(USERNAME)))
                .thenReturn(chatRoom);

        ChatRoom expectedChatRoom = new ChatRoom();
        expectedChatRoom.setRoomId(chatRoomDto.getRoomId());
        expectedChatRoom.setName(chatRoomDto.getName());
        expectedChatRoom.setDescription(chatRoomDto.getDescription());
        expectedChatRoom.setPrivate(chatRoomDto.isPrivate());
        expectedChatRoom.setOwnerUsername(USERNAME);

        when(chatRoomService.createChatRoom(eq(chatRoomDto), eq(USERNAME)))
                .thenReturn(expectedChatRoom);

        mockMvc.perform(post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRoomDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId", is(chatRoomDto.getRoomId())))
                .andExpect(jsonPath("$.name", is(ROOM_NAME)))
                .andExpect(jsonPath("$.ownerUsername", is(USERNAME)));

        verify(chatRoomService).createChatRoom(eq(chatRoomDto), eq(USERNAME));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getChatRoom_Success() throws Exception {
        when(chatRoomService.getChatRoomById(ROOM_ID)).thenReturn(chatRoom);

        mockMvc.perform(get("/api/v1/rooms/{roomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(ROOM_ID)))
                .andExpect(jsonPath("$.name", is(ROOM_NAME)));

        verify(chatRoomService).getChatRoomById(ROOM_ID);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getChatRoom_NotFound() throws Exception {
        when(chatRoomService.getChatRoomById(ROOM_ID))
                .thenThrow(new ResourceNotFoundException("Чат-комната не найдена"));

        mockMvc.perform(get("/api/v1/rooms/{roomId}", ROOM_ID))
                .andExpect(status().isNotFound());

        verify(chatRoomService).getChatRoomById(ROOM_ID);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getPublicRooms_Success() throws Exception {
        List<ChatRoom> rooms = Arrays.asList(chatRoom);
        when(chatRoomService.getPublicChatRooms()).thenReturn(rooms);

        mockMvc.perform(get("/api/v1/rooms/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].roomId", is(ROOM_ID)));

        verify(chatRoomService).getPublicChatRooms();
    }

    @Test
    @WithMockUser(username = "testUser")
    void getPublicRooms_EmptyList() throws Exception {
        when(chatRoomService.getPublicChatRooms()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/rooms/public"))
                .andExpect(status().isNoContent());

        verify(chatRoomService).getPublicChatRooms();
    }

    @Test
    @WithMockUser(username = "testUser")
    void getUserRooms_Success() throws Exception {
        List<ChatRoom> rooms = Arrays.asList(chatRoom);
        when(chatRoomService.getUserChatRooms(USERNAME)).thenReturn(rooms);

        mockMvc.perform(get("/api/v1/rooms/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].roomId", is(ROOM_ID)));

        verify(chatRoomService).getUserChatRooms(USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void addMember_Success() throws Exception {
        String memberUsername = "newMember";
        doNothing().when(chatRoomService).addMemberToChatRoom(ROOM_ID, memberUsername, USERNAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/members", ROOM_ID)
                .param("username", memberUsername)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(chatRoomService).addMemberToChatRoom(ROOM_ID, memberUsername, USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void addMember_Unauthorized() throws Exception {
        String memberUsername = "newMember";
        doThrow(new UnauthorizedException("Вы не являетесь владельцем этой комнаты"))
                .when(chatRoomService).addMemberToChatRoom(ROOM_ID, memberUsername, USERNAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/members", ROOM_ID)
                .param("username", memberUsername)
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(chatRoomService).addMemberToChatRoom(ROOM_ID, memberUsername, USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void removeMember_Success() throws Exception {
        String memberUsername = "member";
        doNothing().when(chatRoomService).removeMemberFromChatRoom(ROOM_ID, memberUsername, USERNAME);

        mockMvc.perform(delete("/api/v1/rooms/{roomId}/members/{username}", ROOM_ID, memberUsername)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(chatRoomService).removeMemberFromChatRoom(ROOM_ID, memberUsername, USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void leaveRoom_Success() throws Exception {
        doNothing().when(chatRoomService).removeMemberFromChatRoom(ROOM_ID, USERNAME, USERNAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/leave", ROOM_ID)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(chatRoomService).removeMemberFromChatRoom(ROOM_ID, USERNAME, USERNAME);
    }
} 