package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.model.ChatRoom;
import iwkms.chatapp.chatservice.service.ChatRoomService;
import iwkms.chatapp.chatservice.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
                           Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        
        String username = null;
        boolean isAuthenticated = false;
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            username = authentication.getName();
            isAuthenticated = true;
        }
        
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        model.addAttribute("currentUsername", username);

        if (!model.containsAttribute("messageDto")) {
            ChatMessageDto newMessageDto = new ChatMessageDto();
            newMessageDto.setSenderUsername(username);
            if (chatRoomId != null) {
                newMessageDto.setChatRoomId(chatRoomId);
            }
            model.addAttribute("messageDto", newMessageDto);
        }
        
        model.addAttribute("currentChatRoomId", chatRoomId);
        
        List<ChatRoom> publicRooms = chatRoomService.getPublicChatRooms();
        List<ChatRoom> userRooms = chatRoomService.getUserChatRooms(username);
        model.addAttribute("publicRooms", publicRooms);
        model.addAttribute("userRooms", userRooms);

        ChatRoom currentChatRoom = null;
        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            try {
                currentChatRoom = chatRoomService.getChatRoomById(chatRoomId);
                if (currentChatRoom.isPrivate()) {
                    if (!chatRoomService.checkMembership(chatRoomId, username) && !currentChatRoom.getOwnerUsername().equals(username)) {
                        redirectAttributes.addFlashAttribute("errorMessage", "You do not have access to this private room.");
                        return "redirect:/chat?accessDenied=true";
                    }
                }
                model.addAttribute("chatRoom", currentChatRoom);
                List<ChatMessage> messages = chatService.getMessagesByChatRoom(chatRoomId, username);
                model.addAttribute("messages", messages);

            } catch (iwkms.chatapp.chatservice.exception.ResourceNotFoundException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chat room not found: " + chatRoomId);
                return "redirect:/chat?roomNotFound=true";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Error accessing room: " + e.getMessage());
                return "redirect:/chat?error=true";
            }
        } else {
            model.addAttribute("chatRoom", new ChatRoom());
            model.addAttribute("messages", Collections.emptyList());
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