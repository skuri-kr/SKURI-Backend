package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final String SOCKJS_STOMP_ENDPOINT = "/ws";
    private static final String NATIVE_STOMP_ENDPOINT = "/ws-native";

    private final FirebaseStompAuthChannelInterceptor firebaseStompAuthChannelInterceptor;
    private final ChatSubscriptionAccessInterceptor chatSubscriptionAccessInterceptor;
    private final TrackingWebSocketHandlerDecoratorFactory trackingWebSocketHandlerDecoratorFactory;
    private final ChatWebSocketHandshakeLoggingInterceptor chatWebSocketHandshakeLoggingInterceptor;
    @Value("${chat.websocket.allowed-origin-patterns:}")
    private String[] allowedOriginPatterns;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setPreservePublishOrder(true);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        applyAllowedOriginPatterns(registry.addEndpoint(NATIVE_STOMP_ENDPOINT))
                .addInterceptors(chatWebSocketHandshakeLoggingInterceptor);
        applyAllowedOriginPatterns(registry.addEndpoint(SOCKJS_STOMP_ENDPOINT))
                .addInterceptors(chatWebSocketHandshakeLoggingInterceptor)
                .withSockJS();
    }

    private StompWebSocketEndpointRegistration applyAllowedOriginPatterns(
            StompWebSocketEndpointRegistration endpoint
    ) {
        if (allowedOriginPatterns != null && Arrays.stream(allowedOriginPatterns).anyMatch(value -> value != null && !value.isBlank())) {
            endpoint.setAllowedOriginPatterns(allowedOriginPatterns);
        }
        return endpoint;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(firebaseStompAuthChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatSubscriptionAccessInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(trackingWebSocketHandlerDecoratorFactory);
    }
}
