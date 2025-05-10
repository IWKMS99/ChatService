package iwkms.chatapp.common.security.websocket;

import iwkms.chatapp.common.security.jwt.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Базовый перехватчик для WebSocket каналов, обеспечивающий JWT аутентификацию.
 * Используется при настройке WebSocketMessageBrokerConfigurer.configureClientInboundChannel.
 */
public class JwtWebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public JwtWebSocketChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Используем централизованный метод из JwtUtil
                Authentication authentication = jwtUtil.getAuthentication(token);

                if (authentication != null) {
                    accessor.setUser(authentication);
                    // SecurityContextHolder может не пробрасываться автоматически в потоки обработки WebSocket сообщений
                    // в зависимости от настройки. Устанавливать здесь - хорошая мера.
                    // Однако для @MessageMapping методов часто предпочтительнее использовать SimpMessageHeaderAccessor.getUser().
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // Если authentication равен null, токен недействительный или просрочен.
                // Соединение, вероятно, завершится ошибкой или продолжится без аутентификации,
                // в зависимости от настройки безопасности.
            }
        }
        return message;
    }
} 