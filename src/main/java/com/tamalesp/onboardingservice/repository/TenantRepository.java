package com.tamalesp.onboardingservice.repository;

import com.tamalesp.onboardingservice.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    /**
     * Finds a Tenant by its unique business identifier (tenantId).
     * @param tenantId The unique string identifier for the tenant.
     * @return An Optional containing the Tenant if found, or an empty Optional otherwise.
     */

    Optional<Tenant> findByTenantId (String tenantId);
}
