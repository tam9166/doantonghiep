package poly.edu.quanlynhahang.config;

import java.security.Principal;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import poly.edu.quanlynhahang.security.CustomUserDetailsService;
import poly.edu.quanlynhahang.security.JwtUtils;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.service.PaymentCapabilityService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final ReservationRepository reservationRepository;
    private final PaymentCapabilityService capabilityService;
    private final List<String> allowedOrigins;

    public WebSocketConfig(JwtUtils jwtUtils,
                           CustomUserDetailsService userDetailsService,
                           ReservationRepository reservationRepository,
                           PaymentCapabilityService capabilityService,
                           @Value("${app.cors.allowed-origins:}") String origins) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.reservationRepository = reservationRepository;
        this.capabilityService = capabilityService;
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor);
                }
                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    authorizeClientSend(accessor);
                }
                return message;
            }
        });
    }

    void authorizeClientSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if ("/app/order/cancel".equals(destination)) {
            requireAnyRole(accessor.getUser(), destination,
                    "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER");
            return;
        }
        throw new AccessDeniedException("Client send destination is not allowed");
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = bearerToken(accessor.getFirstNativeHeader("Authorization"));
        if (!StringUtils.hasText(token) || !jwtUtils.validateJwtToken(token)) {
            return;
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
    }

    void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new AccessDeniedException("Subscription destination is required");
        }
        if (destination.startsWith("/topic/reservations/")) {
            authorizeReservationSubscription(accessor, destination);
            return;
        }
        if (destination.startsWith("/topic/admin/")) {
            requireAnyRole(accessor.getUser(), destination, "ROLE_ADMIN", "ROLE_MANAGER");
            return;
        }
        if (destination.equals("/topic/kitchen")) {
            requireAnyRole(accessor.getUser(), destination,
                    "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_CASHIER");
            return;
        }
        if (destination.equals("/topic/waiter")) {
            requireAnyRole(accessor.getUser(), destination,
                    "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_WAITER");
            return;
        }
        if (destination.equals("/topic/orders")) {
            requireAnyRole(accessor.getUser(), destination,
                    "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_WAITER", "ROLE_CASHIER");
            return;
        }
        if (destination.startsWith("/user/queue/")) {
            requireAnyRole(accessor.getUser(), destination,
                    "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER",
                    "ROLE_CASHIER", "ROLE_STAFF", "ROLE_CUSTOMER");
            return;
        }
        throw new AccessDeniedException("Subscription destination is not allowed: " + destination);
    }

    private void authorizeReservationSubscription(StompHeaderAccessor accessor, String destination) {
        String code = destination.substring("/topic/reservations/".length()).trim();
        if (code.isBlank() || code.contains("/")) {
            throw new AccessDeniedException("Invalid reservation subscription");
        }
        Reservation reservation = reservationRepository.findByReservationCode(code)
                .orElseThrow(() -> new AccessDeniedException("Reservation subscription is not allowed"));
        String capability = accessor.getFirstNativeHeader("X-Reservation-Capability");
        if (!StringUtils.hasText(capability)) {
            capability = accessor.getFirstNativeHeader("X-Payment-Capability");
        }
        try {
            capabilityService.authorizeReservationRealtime(reservation, capability, accessor.getUser());
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            throw new AccessDeniedException("Reservation subscription is not allowed");
        }
    }

    private void requireAnyRole(Principal principal, String destination, String... allowedRoles) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required for " + destination);
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (String role : allowedRoles) {
            boolean hasRole = authorities.stream().anyMatch(authority -> role.equals(authority.getAuthority()));
            if (hasRole) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied for " + destination);
    }

    private String bearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }
}
