package iwkms.chatapp.chatservice.controller;

import iwkms.chatapp.chatservice.dto.ChatMessageDto;
import iwkms.chatapp.chatservice.model.ChatMessage;
import iwkms.chatapp.chatservice.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public ChatWebController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chatPage(@RequestParam(name = "username", required = false) String username,
                           @RequestParam(name = "chatRoomId", required = false) String chatRoomId,
                           Model model) {

        model.addAttribute("currentUsername", username);
        model.addAttribute("currentChatRoomId", chatRoomId);
        model.addAttribute("messageDto", new ChatMessageDto());

        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            List<ChatMessage> messages = chatService.getMessagesByChatRoom(chatRoomId);
            model.addAttribute("messages", messages);
        } else {
            model.addAttribute("messages", Collections.emptyList());
        }

        return "chat"; // Имя HTML-файла (chat.html)
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
            redirectAttributes.addFlashAttribute("errorMessage", "Message could not be sent. Please check the fields.");
            return "redirect:/chat";
        }

        if (messageDto.getSenderUsername() == null || messageDto.getSenderUsername().isBlank() ||
                messageDto.getChatRoomId() == null || messageDto.getChatRoomId().isBlank()) {
            redirectAttributes.addAttribute("username", messageDto.getSenderUsername());
            redirectAttributes.addAttribute("chatRoomId", messageDto.getChatRoomId());
            redirectAttributes.addFlashAttribute("errorMessage", "Username and Chat Room ID are required to send a message.");
            return "redirect:/chat";
        }

        chatService.saveMessage(messageDto);

        redirectAttributes.addAttribute("username", messageDto.getSenderUsername());
        redirectAttributes.addAttribute("chatRoomId", messageDto.getChatRoomId());
        return "redirect:/chat";
    }
}