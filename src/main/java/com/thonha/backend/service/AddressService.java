package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.AddressRequest;
import com.thonha.backend.dto.response.AddressResponse;
import com.thonha.backend.entity.Address;
import com.thonha.backend.entity.User;
import com.thonha.backend.repository.AddressRepository;
import com.thonha.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AddressService {
    AddressRepository addressRepository;
    UserRepository userRepository;
    GoongService goongService;



    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = getUser(userId);
        Address address = new Address();
        address.setUser(user);
        copy(address, request);

        if (Boolean.TRUE.equals(request.getDefaultAddress()) || addressRepository.countByUserId(userId) == 0) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long id, AddressRequest request) {
        Address address = own(userId, id);
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        copy(address, request);
        if (Boolean.TRUE.equals(request.getDefaultAddress()) && !wasDefault) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Address address = own(userId, id);
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId)
                    .stream()
                    .findFirst()
                    .ifPresent(a -> a.setDefaultAddress(true));
        }
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long id) {
        Address address = own(userId, id);
        clearDefault(userId);
        address.setDefaultAddress(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.USER_NOT_FOUND));
    }

    private Address own(Long userId, Long id) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        ErrorCode.NOT_FOUND,
                        "Không tìm thấy địa chỉ"));
    }

    private void clearDefault(Long userId) {
        addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId)
                .forEach(a -> a.setDefaultAddress(false));
    }

    private void copy(Address address, AddressRequest request) {
        String oldFull = address.getFullAddress();
        String newFull = request.getFullAddress().trim();
        address.setLabel(request.getLabel() == null ? null : request.getLabel().trim());
        address.setFullAddress(newFull);
        if (request.getLat() != null && request.getLng() != null) {
            address.setLat(BigDecimal.valueOf(request.getLat()));
            address.setLng(BigDecimal.valueOf(request.getLng()));
            return;
        }
        boolean changed = oldFull == null || !newFull.equals(oldFull);
        boolean hasCoords = address.getLat() != null && address.getLng() != null;
        if (!changed && hasCoords) {
            return;
        }
        GoongService.Coordinate c = goongService.geocode(newFull);
        if (c != null) {
            address.setLat(BigDecimal.valueOf(c.lat));
            address.setLng(BigDecimal.valueOf(c.lng));
        } else if (changed) {
            address.setLat(null);
            address.setLng(null);
        }
    }
}
