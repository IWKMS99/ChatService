package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import iwkms.chatapp.chatservice.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
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
    
    @MockBean
    private ChatService chatService;
    
    @MockBean
    private ChatMessageRepository chatMessageRepository;
    
    @MockBean
    private ChatRoomRepository chatRoomRepository;

    private final String USERNAME = "testUser";
    private final String ROOM_ID = "test-room";
    private final String PRIVATE_ROOM_ID = "private-room";
    private ChatRoom publicRoom;
    private ChatRoom privateRoom;

    @BeforeEach
    void setUp() {
        publicRoom = new ChatRoom();
        publicRoom.setRoomId(ROOM_ID);
        publicRoom.setName("Public Test Room");
        publicRoom.setPrivate(false);
        publicRoom.setOwnerUsername("system");

        privateRoom = new ChatRoom();
        privateRoom.setRoomId(PRIVATE_ROOM_ID);
        privateRoom.setName("Private Test Room");
        privateRoom.setPrivate(true);
        privateRoom.setOwnerUsername(USERNAME);
    }

    @Test
    @WithMockUser(username = "testUser")
    void getMainPage_ShouldReturnChatPage() throws Exception {
        when(chatRoomService.getPublicChatRooms()).thenReturn(Collections.singletonList(publicRoom));
        when(chatRoomService.getUserChatRooms(anyString())).thenReturn(Collections.singletonList(publicRoom));

        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("publicRooms"))
                .andExpect(model().attributeExists("userRooms"))
                .andExpect(model().attribute("currentUsername", "testUser"));
    }

    @Test
    @WithMockUser(username = USERNAME, roles = "USER")
    void getChatRoom_ShouldReturnChatRoomPage() throws Exception {
        when(chatRoomService.getChatRoomById(ROOM_ID)).thenReturn(publicRoom);
        when(chatRoomService.checkMembership(ROOM_ID, USERNAME)).thenReturn(true);
        when(chatService.getMessagesByChatRoom(ROOM_ID, USERNAME)).thenReturn(Collections.emptyList());
        when(chatRoomService.getPublicChatRooms()).thenReturn(Collections.singletonList(publicRoom));
        when(chatRoomService.getUserChatRooms(USERNAME)).thenReturn(Collections.singletonList(publicRoom));

        mockMvc.perform(get("/chat").param("chatRoomId", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attribute("currentChatRoomId", ROOM_ID))
                .andExpect(model().attribute("chatRoom", publicRoom));
    }

    @Test
    @WithMockUser(username = "otherUser", roles = "USER")
    void getChatRoom_NoAccess_ShouldRedirectToMainPage() throws Exception {
        when(chatRoomService.getChatRoomById(PRIVATE_ROOM_ID)).thenReturn(privateRoom);
        when(chatRoomService.checkMembership(PRIVATE_ROOM_ID, "otherUser")).thenReturn(false); 

        mockMvc.perform(get("/chat").param("chatRoomId", PRIVATE_ROOM_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat?accessDenied=true"));
    }

    @Test
    @WithAnonymousUser
    void getMainPage_Unauthenticated_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
} 