package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthRedirectController.class)
class AuthRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ChatMessageRepository chatMessageRepository;
    
    @MockBean
    private ChatRoomRepository chatRoomRepository;

    @Test
    void redirectToLogin_ShouldRedirectToAuthService() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:8080/auth/login?redirect=http://localhost:8081/chat"));
    }
} 