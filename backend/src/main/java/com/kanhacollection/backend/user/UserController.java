package com.kanhacollection.backend.user;

import com.kanhacollection.backend.auth.UserPrincipal;
import com.kanhacollection.backend.auth.dto.UserSummaryResponse;
import com.kanhacollection.backend.common.ApiResponse;
import com.kanhacollection.backend.user.dto.AddressRequest;
import com.kanhacollection.backend.user.dto.AddressResponse;
import com.kanhacollection.backend.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User & Address Profile Module", description = "User profile updates and saved shipping address management")
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile (Name, Phone)")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserSummaryResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/addresses")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all saved shipping addresses for current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AddressResponse> response = userService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/addresses")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a new shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userService.addAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", response));
    }

    @PutMapping("/addresses/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userService.updateAddress(currentUser.getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
    }

    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a saved shipping address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID addressId) {
        userService.deleteAddress(currentUser.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PatchMapping("/addresses/{addressId}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set address as default shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID addressId) {
        AddressResponse response = userService.setDefaultAddress(currentUser.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated", response));
    }
}
