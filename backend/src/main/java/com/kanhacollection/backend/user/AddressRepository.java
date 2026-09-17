package com.kanhacollection.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdOrderByIsDefaultDesc(UUID userId);
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
