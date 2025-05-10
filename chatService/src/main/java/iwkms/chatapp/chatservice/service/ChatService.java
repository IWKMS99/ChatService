package iwkms.chatapp.chatservice.service;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.exception.UnauthorizedException;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.repository.ChatMessageRepository;
import iwkms.chatapp.chatservice.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatService(ChatMessageRepository chatMessageRepository,
                       ChatRoomRepository chatRoomRepository,
                       ChatRoomService chatRoomService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomService = chatRoomService;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessageDto messageDto) {
        if (!chatRoomRepository.existsByRoomId(messageDto.getChatRoomId())) {
            ChatRoom chatRoom = new ChatRoom(
                    messageDto.getChatRoomId(),
                    "Комната " + messageDto.getChatRoomId(),
                    "Автоматически созданная комната",
                    false,
                    messageDto.getSenderUsername()
            );
            chatRoomRepository.save(chatRoom);
        }
        
        if (!chatRoomService.checkMembership(messageDto.getChatRoomId(), messageDto.getSenderUsername())) {
            throw new UnauthorizedException("Вы не являетесь участником этой приватной комнаты");
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderUsername(messageDto.getSenderUsername());
        chatMessage.setChatRoomId(messageDto.getChatRoomId());
        chatMessage.setContent(messageDto.getContent());
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessageRepository.save(chatMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesByChatRoom(String chatRoomId, String username) {
        if (!chatRoomService.checkMembership(chatRoomId, username)) {
            throw new UnauthorizedException("Вы не имеете доступа к этой приватной комнате");
        }
        
        return chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }
}