package shareyourstory.websocket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import shareyourstory.auth.JWT.JWTService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JWTService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // "/queue" es imprescindible para los destinos de usuario: los mensajes
        // privados se envian con convertAndSendToUser(..., "/queue/private", ...),
        // que Spring reescribe a un destino bajo "/queue". Sin este prefijo el
        // broker simple los descarta y el chat privado no llega en tiempo real.
        config.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:5173").withSockJS();
    }

    /**
     * Autentica el canal: el STOMP CONNECT debe traer un JWT valido en la cabecera
     * Authorization. Sin token valido no se abre la sesion WebSocket. Ata la sesion
     * a un usuario (Principal) para futuras autorizaciones por destino.
     *
     * NOTA: la autorizacion por topic todavia no esta (los mensajes privados se
     * difunden a /topic/privateChats/{id}, un modelo que conviene migrar a destinos
     * de usuario /user/queue para que un suscriptor no vea conversaciones ajenas).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String token = (authHeader != null && authHeader.startsWith("Bearer "))
                            ? authHeader.substring(7)
                            : null;

                    if (token == null || !jwtService.validateJwtToken(token)) {
                        throw new MessagingException("No autenticado: se requiere un JWT valido");
                    }

                    String username = jwtService.getUsernameFromToken(token);
                    accessor.setUser(() -> username);
                }
                return message;
            }
        });
    }
}
