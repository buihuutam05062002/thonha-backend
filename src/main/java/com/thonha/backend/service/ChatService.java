package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.response.ChatEvent;
import com.thonha.backend.dto.response.ChatResponse;
import com.thonha.backend.dto.response.MessageResponse;
import com.thonha.backend.entity.Chat;
import com.thonha.backend.entity.Message;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.User;
import com.thonha.backend.enums.MessageType;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.ChatRepository;
import com.thonha.backend.repository.MessageRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
public class ChatService {

    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final int MAX_PAGE_SIZE = 50;
    private static final Set<RepairStatus> CHATTABLE = EnumSet.of(
            RepairStatus.MATCHED, RepairStatus.ON_THE_WAY, RepairStatus.IN_PROGRESS);

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;
    private final AttachmentValidator attachmentValidator;
    private final CloudinaryService cloudinaryService;
    private SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatRepository chatRepository,
                       MessageRepository messageRepository,
                       RepairRequestRepository repairRequestRepository,
                       UserRepository userRepository,
                       AttachmentValidator attachmentValidator,
                       CloudinaryService cloudinaryService) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.userRepository = userRepository;
        this.attachmentValidator = attachmentValidator;
        this.cloudinaryService = cloudinaryService;
    }

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ---------------------------------------------------------------- truy vấn

    /** Lấy (hoặc tạo) cuộc chat của một yêu cầu. Chỉ khách chủ yêu cầu và thợ được giao mới vào được. */
    @Transactional
    public ChatResponse getOrCreateByRequest(Long requestId, Long userId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        if (request.getWorker() == null) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Yêu cầu chưa có thợ nhận nên chưa thể nhắn tin");
        }
        User customer = request.getCustomer();
        User worker = request.getWorker().getUser();
        if (!userId.equals(customer.getId()) && !userId.equals(worker.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không thuộc cuộc trò chuyện này");
        }
        Chat chat = chatRepository.findByRequestIdAndWorkerId(requestId, worker.getId())
                .orElseGet(() -> chatRepository.save(Chat.builder()
                        .request(request).customer(customer).worker(worker).build()));
        return toResponse(chat, userId);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long chatId, Long userId, Long beforeId, int size) {
        Chat chat = requireMember(chatId, userId);
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<Message> page = new ArrayList<>(messageRepository.findPage(chat.getId(), beforeId, PageRequest.of(0, limit)));
        Collections.reverse(page); // trả theo thứ tự cũ -> mới
        return page.stream().map(MessageResponse::from).toList();
    }

    /** Số tin của người kia mà người dùng này chưa xem trong cuộc chat. */
    @Transactional(readOnly = true)
    public long unseenCount(Long chatId, Long userId) {
        Chat chat = requireMember(chatId, userId);
        return messageRepository.countByChatIdAndSenderIdNotAndSeenFalse(chat.getId(), userId);
    }

    // ---------------------------------------------------------------- gửi / đã xem

    @Transactional
    public MessageResponse sendMessage(Long chatId, Long userId, String content, List<MultipartFile> files) {
        Chat chat = requireMember(chatId, userId);
        if (!CHATTABLE.contains(chat.getRequest().getStatus())) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Yêu cầu đã kết thúc, không thể gửi thêm tin nhắn");
        }

        String text = content == null ? null : content.trim();
        if (text != null && text.isEmpty()) {
            text = null;
        }
        if (text != null && text.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tin nhắn tối đa " + MAX_CONTENT_LENGTH + " ký tự");
        }
        List<AttachmentValidator.Validated> images = attachmentValidator.validate(files);
        for (AttachmentValidator.Validated v : images) {
            if (v.kind() != AttachmentValidator.Kind.IMAGE) {
                throw new ApiException(ErrorCode.INVALID_FILE_TYPE, "Chat chỉ hỗ trợ gửi ảnh JPG, PNG, WEBP");
            }
        }
        if (text == null && images.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tin nhắn không được để trống");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(text)
                .messageType(images.isEmpty() ? MessageType.TEXT : MessageType.IMAGE)
                .build();

        List<CloudinaryService.UploadResult> uploaded = new ArrayList<>();
        try {
            for (AttachmentValidator.Validated v : images) {
                CloudinaryService.UploadResult r = cloudinaryService.uploadAs(
                        v.file(), "nhatho/chat/" + chat.getId(), "image");
                uploaded.add(r);
                message.addImage(r.url());
            }
            message = messageRepository.saveAndFlush(message);
        } catch (RuntimeException e) {
            uploaded.forEach(cloudinaryService::deleteQuietly); // không để ảnh mồ côi trên Cloudinary
            throw e;
        }

        MessageResponse response = MessageResponse.from(message);
        push(otherUserId(chat, userId), ChatEvent.builder()
                .type("NEW_MESSAGE").chatId(chat.getId()).requestId(chat.getRequest().getId())
                .message(response).build());
        return response;
    }

    /** Đánh dấu mọi tin của người kia là đã xem. */
    @Transactional
    public void markSeen(Long chatId, Long userId) {
        Chat chat = requireMember(chatId, userId);
        int updated = messageRepository.markSeen(chat.getId(), userId);
        if (updated > 0) {
            push(otherUserId(chat, userId), ChatEvent.builder()
                    .type("MESSAGES_SEEN").chatId(chat.getId()).requestId(chat.getRequest().getId()).build());
        }
    }

    // ---------------------------------------------------------------- nội bộ

    private Chat requireMember(Long chatId, Long userId) {
        Chat chat = chatRepository.findByIdWithDetails(chatId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy cuộc trò chuyện"));
        if (!userId.equals(chat.getCustomer().getId()) && !userId.equals(chat.getWorker().getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không thuộc cuộc trò chuyện này");
        }
        return chat;
    }

    private Long otherUserId(Chat chat, Long userId) {
        return userId.equals(chat.getCustomer().getId()) ? chat.getWorker().getId() : chat.getCustomer().getId();
    }

    private ChatResponse toResponse(Chat chat, Long userId) {
        User other = userId.equals(chat.getCustomer().getId()) ? chat.getWorker() : chat.getCustomer();
        MessageResponse last = messageRepository.findFirstByChatIdOrderByIdDesc(chat.getId())
                .map(MessageResponse::from).orElse(null);
        return ChatResponse.builder()
                .id(chat.getId())
                .requestId(chat.getRequest().getId())
                .requestCode(chat.getRequest().getRequestCode())
                .otherUserId(other.getId())
                .otherUserName(other.getFullName())
                .otherUserAvatarUrl(other.getAvatarUrl())
                .canSend(CHATTABLE.contains(chat.getRequest().getStatus()))
                .unreadCount(messageRepository.countByChatIdAndSenderIdNotAndSeenFalse(chat.getId(), userId))
                .lastMessage(last)
                .build();
    }

    /** Đẩy realtime SAU KHI COMMIT; lỗi WebSocket không được làm hỏng việc gửi tin. */
    private void push(Long recipientUserId, ChatEvent event) {
        if (messagingTemplate == null) {
            return;
        }
        Runnable send = () -> {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + recipientUserId + "/chat", event);
            } catch (Exception e) {
                log.warn("Đẩy chat realtime thất bại (user {}): {}", recipientUserId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
