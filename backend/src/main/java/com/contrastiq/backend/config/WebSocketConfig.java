package com.contrastiq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Habilita STOMP sobre WebSocket (con fallback SockJS) para empujar
// alertas al frontend en tiempo real, en vez de que alguien tenga que
// entrar al dashboard a revisarlas. El backend publica en /topic/alertas
// (ver AlertaService.crear) y Angular se suscribe ahi.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.origenes-permitidos:http://localhost:4200,http://127.0.0.1:4200,"
            + "http://192.168.*.*:4200,http://10.*.*.*:4200}")
    private String origenesPermitidos;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket nativo (sin SockJS): mas simple y sin dependencias
        // adicionales del lado del navegador. Todos los navegadores
        // modernos soportan WebSocket nativo, asi que el fallback por
        // HTTP polling de SockJS no hace falta aqui.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origenesPermitidos.split(","));
    }
}
