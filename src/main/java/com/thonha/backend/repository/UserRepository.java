package com.thonha.backend.repository;

import com.thonha.backend.entity.User;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.thonha.backend.enums.UserStatus;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhoneNumber(String phone);

    Optional<User> findByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phone);

    boolean existsByUsername(String username);

    @Query("select u.status from User u where u.id = :id")
    Optional<UserStatus> findStatusById(@Param("id") Long id);

    @Query("select distinct u.id from User u join u.roles r where r.name = :roleName and u.status = :status")
    java.util.List<Long> findIdsByRoleAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);

    @Query("select count(distinct u.id) from User u join u.roles r where r.name = :roleName and u.status <> :excluded")
    long countByRole(@Param("roleName") String roleName, @Param("excluded") UserStatus excluded);

    @Query("select count(distinct u.id) from User u join u.roles r where r.name = :roleName and u.status <> :excluded " +
            "and u.createdAt >= :from and u.createdAt < :to")
    long countByRoleCreatedBetween(@Param("roleName") String roleName, @Param("excluded") UserStatus excluded,
                                   @Param("from") java.time.LocalDateTime from,
                                   @Param("to") java.time.LocalDateTime to);
}
