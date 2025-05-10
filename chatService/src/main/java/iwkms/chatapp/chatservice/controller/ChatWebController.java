package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import iwkms.chatapp.chatservice.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class ChatWebController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatWebController(ChatService chatService, ChatRoomService chatRoomService) {
        this.chatService = chatService;
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/chat")
    public String chatPage(@RequestParam(name = "chatRoomId", required = false) String chatRoomId,
                           Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null && authentication.isAuthenticated() && 
                          !authentication.getPrincipal().equals("anonymousUser") ? 
                          authentication.getName() : null;
        
        boolean isAuthenticated = username != null;
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (!model.containsAttribute("messageDto")) {
            ChatMessageDto newMessageDto = new ChatMessageDto();
            if (isAuthenticated) {
                newMessageDto.setSenderUsername(username);
                if (chatRoomId != null) {
                    newMessageDto.setChatRoomId(chatRoomId);
                }
            }
            model.addAttribute("messageDto", newMessageDto);
        }

        model.addAttribute("currentUsername", username);
        model.addAttribute("currentChatRoomId", chatRoomId);
        
        // Default empty chat room to prevent null pointer exceptions
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setMembers(Collections.emptySet());
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("messages", Collections.emptyList());
        
        // Получаем комнаты и сообщения только для аутентифицированных пользователей
        if (isAuthenticated) {
            // Получить список доступных комнат
            List<ChatRoom> publicRooms = chatRoomService.getPublicChatRooms();
            List<ChatRoom> userRooms = chatRoomService.getUserChatRooms(username);
            model.addAttribute("publicRooms", publicRooms);
            model.addAttribute("userRooms", userRooms);

            if (chatRoomId != null && !chatRoomId.isEmpty()) {
                try {
                    List<ChatMessage> messages = chatService.getMessagesByChatRoom(chatRoomId, username);
                    model.addAttribute("messages", messages);
                    
                    // Get chat room and update the model attribute with it
                    ChatRoom fetchedRoom = chatRoomService.getChatRoomById(chatRoomId);
                    if (fetchedRoom != null) {
                        model.addAttribute("chatRoom", fetchedRoom);
                    }
                } catch (Exception e) {
                    model.addAttribute("errorMessage", e.getMessage());
                }
            }
        } else {
            model.addAttribute("loginRequired", true);
            model.addAttribute("publicRooms", Collections.emptyList());
            model.addAttribute("userRooms", Collections.emptyList());
        }

        return "chat";
    }

    @PostMapping("/chat/send")
    public String sendMessage(@Valid @ModelAttribute("messageDto") ChatMessageDto messageDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.messageDto", bindingResult);
            redirectAttributes.addFlashAttribute("messageDto", messageDto);

            redirectAttributes.addAttribute("username", messageDto.getSenderUsername());
            redirectAttributes.addAttribute("chatRoomId", messageDto.getChatRoomId());

            return "redirect:/chat";
        }

        chatService.saveMessage(messageDto);

        redirectAttributes.addAttribute("username", messageDto.getSenderUsername());
        redirectAttributes.addAttribute("chatRoomId", messageDto.getChatRoomId());
        return "redirect:/chat";
    }
}