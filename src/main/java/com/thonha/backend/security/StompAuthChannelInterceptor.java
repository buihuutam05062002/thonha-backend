package com.thonha.backend.security;

import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/user/(\\d+)/[\\w-]+$");
    /** Hàng đợi riêng của từng user (Spring tự gắn theo Principal): tin vừa gửi + lỗi. */
    private static final Pattern USER_QUEUE = Pattern.compile("^/user/queue/(chat|errors)$");
    private static final Pattern CHAT_SEND = Pattern.compile("^/app/chats/\\d+/messages$");
    /** Kênh vị trí của một yêu cầu: chỉ khách hoặc thợ được ghép của yêu cầu đó mới được nghe. */
    private static final Pattern REQUEST_TOPIC = Pattern.compile("^/topic/requests/(\\d+)/location$");
    /** Gửi vị trí: chỉ thợ phụ trách (TrackingService kiểm tra tiếp trạng thái đơn). */
    private static final Pattern LOCATION_SEND = Pattern.compile("^/app/requests/(\\d+)/location$");

    private final JwtService jwt;
    private final UserRepository userRepository;
    private final RepairRequestRepository repairRequestRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            authorizeSend(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Thiếu access token");
        }
        try {
            Claims claims = jwt.parse(header.substring(7));
            if (!"access".equals(claims.get("type", String.class))) {
                throw new MessageDeliveryException("Token không hợp lệ");
            }
            Long id = Long.valueOf(claims.getSubject());
            if (userRepository.findStatusById(id).orElse(null) != UserStatus.ACTIVE) {
                throw new MessageDeliveryException("Tài khoản không còn hoạt động");
            }
            String userId = String.valueOf(id);
            Principal principal = () -> userId;
            accessor.setUser(principal);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.debug("STOMP CONNECT bị từ chối: {}", e.getMessage());
            throw new MessageDeliveryException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null) {
            throw new MessageDeliveryException("Chưa xác thực");
        }
        if (USER_QUEUE.matcher(destination).matches()) {
            return;
        }
        Matcher own = USER_TOPIC.matcher(destination);
        if (own.matches() && own.group(1).equals(user.getName())) {
            return;
        }
        Matcher req = REQUEST_TOPIC.matcher(destination);
        if (req.matches() && isParticipant(Long.valueOf(user.getName()), Long.valueOf(req.group(1)))) {
            return;
        }
        throw new MessageDeliveryException("Không có quyền đăng ký kênh này");
    }

    /**
     * Cho SEND khi đã xác thực tới:
     *  - /app/chats/{chatId}/messages: thuộc cuộc chat, yêu cầu còn mở... do ChatService kiểm tra.
     *  - /app/requests/{id}/location: phải là thợ của yêu cầu; đúng trạng thái và tọa độ do TrackingService kiểm tra.
     * Interceptor chỉ chặn sớm người lạ, luật nghiệp vụ chỉ có một chỗ để sửa.
     */
    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (accessor.getUser() == null || destination == null) {
            throw new MessageDeliveryException("Chưa xác thực");
        }
        if (CHAT_SEND.matcher(destination).matches()) {
            return;
        }
        Matcher loc = LOCATION_SEND.matcher(destination);
        if (loc.matches() && repairRequestRepository.existsByIdAndWorker_User_Id(
                Long.valueOf(loc.group(1)), Long.valueOf(accessor.getUser().getName()))) {
            return;
        }
        throw new MessageDeliveryException("Client không được phép gửi message tới kênh này");
    }

    private boolean isParticipant(Long userId, Long requestId) {
        return repairRequestRepository.existsByIdAndCustomerId(requestId, userId)
                || repairRequestRepository.existsByIdAndWorker_User_Id(requestId, userId);
    }
}
