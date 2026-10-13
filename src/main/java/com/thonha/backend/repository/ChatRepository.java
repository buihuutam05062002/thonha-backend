package com.thonha.backend.repository;

import com.thonha.backend.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByRequestIdAndWorkerId(Long requestId, Long workerUserId);

    @Query("select c from Chat c join fetch c.customer join fetch c.worker join fetch c.request where c.id = :id")
    Optional<Chat> findByIdWithDetails(@Param("id") Long id);
}
