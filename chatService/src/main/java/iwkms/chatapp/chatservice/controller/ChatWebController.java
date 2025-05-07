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

        if (!model.containsAttribute("messageDto")) {
            ChatMessageDto newMessageDto = new ChatMessageDto();
            if (username != null) {
                newMessageDto.setSenderUsername(username);
            }
            if (chatRoomId != null) {
                newMessageDto.setChatRoomId(chatRoomId);
            }
            model.addAttribute("messageDto", newMessageDto);
        }

        model.addAttribute("currentUsername", username);
        model.addAttribute("currentChatRoomId", chatRoomId);

        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            List<ChatMessage> messages = chatService.getMessagesByChatRoom(chatRoomId);
            model.addAttribute("messages", messages);
        } else {
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