package com.thonha.backend.repository;

import com.thonha.backend.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m join fetch m.sender " +
            "where m.chat.id = :chatId and (:beforeId is null or m.id < :beforeId) " +
            "order by m.id desc")
    List<Message> findPage(@Param("chatId") Long chatId, @Param("beforeId") Long beforeId, Pageable pageable);

    Optional<Message> findFirstByChatIdOrderByIdDesc(Long chatId);

    long countByChatIdAndSenderIdNotAndSeenFalse(Long chatId, Long senderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Message m set m.seen = true where m.chat.id = :chatId and m.sender.id <> :userId and m.seen = false")
    int markSeen(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
