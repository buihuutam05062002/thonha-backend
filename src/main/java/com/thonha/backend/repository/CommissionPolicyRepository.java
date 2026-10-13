package com.thonha.backend.repository;

import com.thonha.backend.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByScopeKey(String scopeKey);

    Optional<CommissionPolicy> findByScopeKeyAndActiveTrue(String scopeKey);

    @EntityGraph(attributePaths = "category")
    List<CommissionPolicy> findAllByOrderByIdAsc();
}
