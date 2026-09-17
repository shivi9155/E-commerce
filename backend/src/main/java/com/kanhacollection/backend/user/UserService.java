package com.kanhacollection.backend.user;

import com.kanhacollection.backend.auth.dto.UserSummaryResponse;
import com.kanhacollection.backend.user.dto.AddressRequest;
import com.kanhacollection.backend.user.dto.AddressResponse;
import com.kanhacollection.backend.user.dto.UpdateProfileRequest;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserSummaryResponse updateProfile(UUID userId, UpdateProfileRequest request);
    List<AddressResponse> getUserAddresses(UUID userId);
    AddressResponse addAddress(UUID userId, AddressRequest request);
    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID userId, UUID addressId);
    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
