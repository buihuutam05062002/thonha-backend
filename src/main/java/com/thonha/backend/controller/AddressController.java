package com.thonha.backend.controller;

import com.thonha.backend.dto.request.AddressRequest;
import com.thonha.backend.dto.response.AddressResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService addressService;
    private final CurrentUserProvider currentUserProvider;

    public AddressController(AddressService addressService, CurrentUserProvider currentUserProvider) {
        this.addressService = addressService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<com.thonha.backend.common.ApiResponse<List<AddressResponse>>> list() {
        return ResponseEntity.ok(com.thonha.backend.common.ApiResponse.success(addressService.list(currentUserProvider.requireUserId())));
    }

    @PostMapping
    public ResponseEntity<com.thonha.backend.common.ApiResponse<AddressResponse>> create(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(com.thonha.backend.common.ApiResponse.success(addressService.create(currentUserProvider.requireUserId(), request), "Tạo địa chỉ thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.thonha.backend.common.ApiResponse<AddressResponse>> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(com.thonha.backend.common.ApiResponse.success(addressService.update(currentUserProvider.requireUserId(), id, request), "Cập nhật địa chỉ thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<com.thonha.backend.common.ApiResponse<Void>> delete(@PathVariable Long id) {
        addressService.delete(currentUserProvider.requireUserId(), id);
        return ResponseEntity.ok(com.thonha.backend.common.ApiResponse.success(null, "Xóa địa chỉ thành công"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<com.thonha.backend.common.ApiResponse<AddressResponse>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(com.thonha.backend.common.ApiResponse.success(addressService.setDefault(currentUserProvider.requireUserId(), id), "Đặt làm địa chỉ mặc định thành công"));
    }
}