package com.thonha.backend.repository;

import com.thonha.backend.entity.Address;

import java.util.*;

import org.springframework.data.jpa.repository.*;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
