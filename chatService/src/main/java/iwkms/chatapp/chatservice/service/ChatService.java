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
        // Проверить, существует ли комната
        if (!chatRoomRepository.existsByRoomId(messageDto.getChatRoomId())) {
            // Если комнаты нет, создадим публичную комнату с этим ID и текущим пользователем как владельцем
            ChatRoom chatRoom = new ChatRoom(
                    messageDto.getChatRoomId(),
                    "Комната " + messageDto.getChatRoomId(),
                    "Автоматически созданная комната",
                    false,
                    messageDto.getSenderUsername() // Отправитель становится владельцем
            );
            chatRoomRepository.save(chatRoom);
        }
        
        // Проверить право на отправку сообщений
        if (!chatRoomService.checkMembership(messageDto.getChatRoomId(), messageDto.getSenderUsername())) {
            throw new UnauthorizedException("Вы не являетесь участником этой приватной комнаты");
        }

        // Сохранить сообщение
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderUsername(messageDto.getSenderUsername());
        chatMessage.setChatRoomId(messageDto.getChatRoomId());
        chatMessage.setContent(messageDto.getContent());
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessageRepository.save(chatMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesByChatRoom(String chatRoomId, String username) {
        // Проверить право на чтение сообщений
        if (!chatRoomService.checkMembership(chatRoomId, username)) {
            throw new UnauthorizedException("Вы не имеете доступа к этой приватной комнате");
        }
        
        return chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }
}