package iwkms.chatapp.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iwkms.chatapp.chatservice.dto.ChatRoomDto;
import iwkms.chatapp.chatservice.exception.ResourceNotFoundException;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatRoom;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private ObjectMapper objectMapper = new ObjectMapper();
    private ChatRoomDto chatRoomDto;
    private ChatRoom chatRoom;
    private final String ROOM_ID = "test-room";
    private final String USER_NAME = "testUser";

    @BeforeEach
    void setUp() {
        chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomId(ROOM_ID);
        chatRoomDto.setName("Test Room");
        chatRoomDto.setDescription("Test Description");
        chatRoomDto.setPrivate(false);

        chatRoom = new ChatRoom(ROOM_ID, "Test Room", "Test Description", false, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void createChatRoom_Success() throws Exception {
        when(chatRoomService.createChatRoom(any(ChatRoomDto.class), anyString()))
                .thenReturn(chatRoom);

        mockMvc.perform(post("/api/v1/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRoomDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId", is(ROOM_ID)))
                .andExpect(jsonPath("$.name", is("Test Room")));

        verify(chatRoomService).createChatRoom(any(ChatRoomDto.class), eq(USER_NAME));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getChatRoom_Success() throws Exception {
        when(chatRoomService.getChatRoomById(ROOM_ID)).thenReturn(chatRoom);

        mockMvc.perform(get("/api/v1/rooms/{roomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(ROOM_ID)));

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(chatRoomService).getPublicChatRooms();
    }

    @Test
    @WithMockUser(username = "testUser")
    void getUserRooms_Success() throws Exception {
        List<ChatRoom> rooms = Arrays.asList(chatRoom);
        when(chatRoomService.getUserChatRooms(USER_NAME)).thenReturn(rooms);

        mockMvc.perform(get("/api/v1/rooms/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].roomId", is(ROOM_ID)));

        verify(chatRoomService).getUserChatRooms(USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void addMember_Success() throws Exception {
        String newMember = "newUser";
        doNothing().when(chatRoomService).addMemberToChatRoom(ROOM_ID, newMember, USER_NAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/members", ROOM_ID)
                .with(csrf())
                .param("username", newMember))
                .andExpect(status().isOk());

        verify(chatRoomService).addMemberToChatRoom(ROOM_ID, newMember, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void addMember_Unauthorized() throws Exception {
        String newMember = "newUser";
        doThrow(new UnauthorizedException("Только участники комнаты могут добавлять новых участников"))
                .when(chatRoomService).addMemberToChatRoom(ROOM_ID, newMember, USER_NAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/members", ROOM_ID)
                .with(csrf())
                .param("username", newMember))
                .andExpect(status().isForbidden());

        verify(chatRoomService).addMemberToChatRoom(ROOM_ID, newMember, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void removeMember_Success() throws Exception {
        String memberToRemove = "userToRemove";
        doNothing().when(chatRoomService).removeMemberFromChatRoom(ROOM_ID, memberToRemove, USER_NAME);

        mockMvc.perform(delete("/api/v1/rooms/{roomId}/members/{username}", ROOM_ID, memberToRemove)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(chatRoomService).removeMemberFromChatRoom(ROOM_ID, memberToRemove, USER_NAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void leaveRoom_Success() throws Exception {
        doNothing().when(chatRoomService).removeMemberFromChatRoom(ROOM_ID, USER_NAME, USER_NAME);

        mockMvc.perform(post("/api/v1/rooms/{roomId}/leave", ROOM_ID)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(chatRoomService).removeMemberFromChatRoom(ROOM_ID, USER_NAME, USER_NAME);
    }
} 