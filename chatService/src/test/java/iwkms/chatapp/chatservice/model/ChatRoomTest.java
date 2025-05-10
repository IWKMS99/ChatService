package iwkms.chatapp.chatservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomTest {

    private ChatRoom chatRoom;
    private final String ROOM_ID = "test-room";
    private final String OWNER_USERNAME = "owner";
    private final String MEMBER_USERNAME = "member";
    
    @BeforeEach
    void setUp() {
        chatRoom = new ChatRoom(ROOM_ID, "Test Room", "Test Description", false, OWNER_USERNAME);
    }
    
    @Test
    void constructor_ShouldAddOwnerAsMember() {
        assertTrue(chatRoom.hasMember(OWNER_USERNAME));
        assertEquals(1, chatRoom.getMembers().size());
    }
    
    @Test
    void addMember_ShouldAddNewMember() {
        chatRoom.addMember(MEMBER_USERNAME);
        
        assertTrue(chatRoom.hasMember(MEMBER_USERNAME));
        assertEquals(2, chatRoom.getMembers().size());
    }
    
    @Test
    void addMember_WhenMemberAlreadyExists_ShouldNotDuplicateMember() {
        chatRoom.addMember(MEMBER_USERNAME);
        chatRoom.addMember(MEMBER_USERNAME);
        
        assertEquals(2, chatRoom.getMembers().size());
    }
    
    @Test
    void removeMember_ShouldRemoveMember() {
        chatRoom.addMember(MEMBER_USERNAME);
        assertTrue(chatRoom.hasMember(MEMBER_USERNAME));
        
        chatRoom.removeMember(MEMBER_USERNAME);
        
        assertFalse(chatRoom.hasMember(MEMBER_USERNAME));
        assertEquals(1, chatRoom.getMembers().size());
    }
    
    @Test
    void removeMember_WhenOwner_ShouldNotRemoveOwner() {
        chatRoom.removeMember(OWNER_USERNAME);
        
        assertTrue(chatRoom.hasMember(OWNER_USERNAME));
        assertEquals(1, chatRoom.getMembers().size());
    }
    
    @Test
    void hasMember_WhenMemberExists_ShouldReturnTrue() {
        chatRoom.addMember(MEMBER_USERNAME);
        
        assertTrue(chatRoom.hasMember(MEMBER_USERNAME));
    }
    
    @Test
    void hasMember_WhenMemberDoesNotExist_ShouldReturnFalse() {
        assertFalse(chatRoom.hasMember("nonexistent"));
    }
    
    @Test
    void isOwner_WhenUserIsOwner_ShouldReturnTrue() {
        assertTrue(chatRoom.isOwner(OWNER_USERNAME));
    }
    
    @Test
    void isOwner_WhenUserIsNotOwner_ShouldReturnFalse() {
        assertFalse(chatRoom.isOwner(MEMBER_USERNAME));
    }
    
    @Test
    void isOwner_WhenUserIsNull_ShouldReturnFalse() {
        assertFalse(chatRoom.isOwner(null));
    }
} 