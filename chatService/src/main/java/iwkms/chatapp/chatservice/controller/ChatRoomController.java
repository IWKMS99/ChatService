package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatRoomDto;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @PostMapping
    public ResponseEntity<ChatRoom> createChatRoom(@Valid @RequestBody ChatRoomDto chatRoomDto, 
                                                  Authentication authentication) {
        String username = authentication.getName();
        ChatRoom chatRoom = chatRoomService.createChatRoom(chatRoomDto, username);
        return new ResponseEntity<>(chatRoom, HttpStatus.CREATED);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoom> getChatRoom(@PathVariable String roomId, Authentication authentication) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        if (room.isPrivate()) {
            String username = authentication.getName();
            if (!chatRoomService.checkMembership(roomId, username) && !room.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(room);
    }

    @GetMapping("/public")
    public ResponseEntity<List<ChatRoom>> getPublicRooms() {
        List<ChatRoom> publicRooms = chatRoomService.getPublicChatRooms();
        if (publicRooms.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(publicRooms);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChatRoom>> getUserRooms(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(chatRoomService.getUserChatRooms(username));
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<Void> addMember(@PathVariable String roomId, 
                                         @RequestParam String username, 
                                         Authentication authentication) {
        String currentUser = authentication.getName();
        chatRoomService.addMemberToChatRoom(roomId, username, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roomId}/members/{username}")
    public ResponseEntity<Void> removeMember(@PathVariable String roomId, 
                                           @PathVariable String username, 
                                           Authentication authentication) {
        String currentUser = authentication.getName();
        chatRoomService.removeMemberFromChatRoom(roomId, username, currentUser);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomId,
                                        Authentication authentication) {
        String username = authentication.getName();
        chatRoomService.removeMemberFromChatRoom(roomId, username, username);
        return ResponseEntity.ok().build();
    }
} 