package iwkms.chatapp.chatservice.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    private final String SENDER_USERNAME = "testUser";
    private final String ROOM_ID = "test-room";
    private final String CONTENT = "Hello, world!";

    @Test
    void constructor_ShouldSetFieldsCorrectly() {
        ChatMessage message = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        
        assertEquals(SENDER_USERNAME, message.getSenderUsername());
        assertEquals(ROOM_ID, message.getChatRoomId());
        assertEquals(CONTENT, message.getContent());
        assertNotNull(message.getTimestamp());
    }
    
    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        ChatMessage message = new ChatMessage();
        
        message.setId(1L);
        message.setSenderUsername(SENDER_USERNAME);
        message.setChatRoomId(ROOM_ID);
        message.setContent(CONTENT);
        
        LocalDateTime now = LocalDateTime.now();
        message.setTimestamp(now);
        
        assertEquals(1L, message.getId());
        assertEquals(SENDER_USERNAME, message.getSenderUsername());
        assertEquals(ROOM_ID, message.getChatRoomId());
        assertEquals(CONTENT, message.getContent());
        assertEquals(now, message.getTimestamp());
    }
    
    @Test
    void equals_WhenSameObject_ShouldReturnTrue() {
        ChatMessage message = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        
        assertEquals(message, message);
    }
    
    @Test
    void equals_WhenEqualFields_ShouldReturnTrue() {
        ChatMessage message1 = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        message1.setId(1L);
        
        ChatMessage message2 = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        message2.setId(1L);
        
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }
    
    @Test
    void equals_WhenDifferentIds_ShouldReturnFalse() {
        ChatMessage message1 = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        message1.setId(1L);
        
        ChatMessage message2 = new ChatMessage(SENDER_USERNAME, ROOM_ID, CONTENT);
        message2.setId(2L);
        
        assertNotEquals(message1, message2);
    }
} 