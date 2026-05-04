package com.team.apk.Controller;

import com.team.apk.Dto.ChatMessagesResponse;
import com.team.apk.Dto.ChatNotificationStatusView;
import com.team.apk.Dto.ChatReadRequest;
import com.team.apk.Dto.ChatSendMessageRequest;
import com.team.apk.Dto.ChatView;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatController {

    private final UsersRepository usersRepository;
    private final ChatService chatService;

    public ChatController(UsersRepository usersRepository,
                          ChatService chatService) {
        this.usersRepository = usersRepository;
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chat(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        ChatView chatView = chatService.buildChatView(user);

        model.addAttribute("chat", chatView);
        model.addAttribute("currentUser", chatView.getCurrentUser());
        model.addAttribute("activeRelation", chatView.getActiveRelation());
        model.addAttribute("partnerPresence", chatView.getPartnerPresence());
        model.addAttribute("messages", chatView.getMessages());
        return "chat";
    }

    @GetMapping("/chat/messages")
    @ResponseBody
    public ResponseEntity<ChatMessagesResponse> loadMessages(@RequestParam(value = "after", required = false) Long afterMessageId,
                                                             Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(new ChatMessagesResponse(
                chatService.loadMessages(currentUser.getUserId(), afterMessageId),
                chatService.loadPartnerPresence(currentUser.getUserId())));
    }

    @GetMapping("/chat/notifications")
    @ResponseBody
    public ResponseEntity<ChatNotificationStatusView> loadNotificationStatus(Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(chatService.loadNotificationStatus(currentUser.getUserId()));
    }

    @PostMapping("/chat/messages")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody(required = false) ChatSendMessageRequest request,
                                         Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        String content = request != null ? request.getContent() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(currentUser.getUserId(), content));
    }

    @PostMapping("/chat/read")
    @ResponseBody
    public ResponseEntity<ChatNotificationStatusView> markConversationAsRead(@RequestBody(required = false) ChatReadRequest request,
                                                                             Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        Long lastVisibleMessageId = request != null ? request.getLastVisibleMessageId() : null;
        return ResponseEntity.ok(chatService.markConversationAsReadUpTo(currentUser.getUserId(), lastVisibleMessageId));
    }

    private Users loadAuthenticatedUser(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable en base."));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank()) {
                return oauthEmail.trim().toLowerCase();
            }
        }
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase();
    }
}
