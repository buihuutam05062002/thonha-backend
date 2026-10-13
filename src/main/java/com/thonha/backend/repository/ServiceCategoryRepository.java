package com.thonha.backend.repository;

import com.thonha.backend.entity.*;

import java.util.*;

import com.thonha.backend.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByStatusOrderByIdAsc(CategoryStatus status);

    List<ServiceCategory> findByIdInAndStatus(Collection<Long> ids, CategoryStatus status);

    Optional<ServiceCategory> findByIdAndStatus(Long id, CategoryStatus status);

    boolean existsByName(String name);
}
