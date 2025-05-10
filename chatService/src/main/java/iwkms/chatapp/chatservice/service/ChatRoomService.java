package iwkms.chatapp.chatservice.service;

import iwkms.chatapp.chatservice.dto.ChatRoomDto;
import iwkms.chatapp.chatservice.exception.ResourceNotFoundException;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    @Transactional
    public ChatRoom createChatRoom(ChatRoomDto chatRoomDto, String creatorUsername) {
        if (chatRoomRepository.existsByRoomId(chatRoomDto.getRoomId())) {
            throw new IllegalArgumentException("Чат-комната с идентификатором " + chatRoomDto.getRoomId() + " уже существует");
        }

        ChatRoom chatRoom = new ChatRoom(
                chatRoomDto.getRoomId(),
                chatRoomDto.getName(),
                chatRoomDto.getDescription(),
                chatRoomDto.isPrivate(),
                creatorUsername
        );
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public ChatRoom getChatRoomById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат-комната не найдена"));
    }

    @Transactional
    public void addMemberToChatRoom(String roomId, String username, String requestingUsername) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        
        if (!chatRoom.hasMember(requestingUsername)) {
            throw new UnauthorizedException("Только участники комнаты могут добавлять новых участников");
        }
        
        chatRoom.addMember(username);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void removeMemberFromChatRoom(String roomId, String username, String requestingUsername) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        
        if (!chatRoom.isOwner(requestingUsername) && !username.equals(requestingUsername)) {
            throw new UnauthorizedException("Только владелец комнаты может удалять других участников");
        }
        
        if (chatRoom.isOwner(username) && !username.equals(requestingUsername)) {
            throw new UnauthorizedException("Владелец комнаты не может быть удален");
        }
        
        chatRoom.removeMember(username);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void addMemberToChatRoom(String roomId, String username) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        chatRoom.addMember(username);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void removeMemberFromChatRoom(String roomId, String username) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        chatRoom.removeMember(username);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public boolean checkMembership(String roomId, String username) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        if (!chatRoom.isPrivate()) {
            return true;
        }
        return chatRoom.hasMember(username);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getPublicChatRooms() {
        return chatRoomRepository.findByIsPrivateFalse();
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getUserChatRooms(String username) {
        return chatRoomRepository.findByMembersContains(username);
    }
} 