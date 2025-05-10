package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatWebController.class)
class ChatWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;

    @Test
    @WithMockUser(username = "testUser")
    void getMainPage_ShouldReturnChatPage() throws Exception {
        ChatRoom chatRoom = new ChatRoom("general", "General", "Public chat room", false, "system");
        when(chatRoomService.getPublicChatRooms()).thenReturn(Collections.singletonList(chatRoom));
        when(chatRoomService.getUserChatRooms(anyString())).thenReturn(Collections.singletonList(chatRoom));

        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("publicRooms"))
                .andExpect(model().attributeExists("userRooms"))
                .andExpect(model().attribute("currentUsername", "testUser"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getChatRoom_ShouldReturnChatRoomPage() throws Exception {
        String roomId = "test-room";
        ChatRoom chatRoom = new ChatRoom(roomId, "Test Room", "Test Description", false, "owner");
        when(chatRoomService.getChatRoomById(roomId)).thenReturn(chatRoom);
        when(chatRoomService.checkMembership(roomId, "testUser")).thenReturn(true);

        mockMvc.perform(get("/chat/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(view().name("chatroom"))
                .andExpect(model().attributeExists("room"))
                .andExpect(model().attribute("currentUsername", "testUser"));
    }

    @Test
    @WithMockUser(username = "testUser")
    void getChatRoom_NoAccess_ShouldRedirectToMainPage() throws Exception {
        String roomId = "private-room";
        ChatRoom chatRoom = new ChatRoom(roomId, "Private Room", "Private chat room", true, "owner");
        when(chatRoomService.getChatRoomById(roomId)).thenReturn(chatRoom);
        when(chatRoomService.checkMembership(roomId, "testUser")).thenReturn(false);

        mockMvc.perform(get("/chat/{roomId}", roomId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat"));
    }

    @Test
    void getMainPage_Unauthenticated_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().is3xxRedirection());
    }
} 