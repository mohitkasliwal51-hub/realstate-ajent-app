package com.bhartiyasaas.stayfile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByEmail(String email);
    Optional<Profile> findByOrganizationIdAndEmail(UUID organizationId, String email);
    Optional<Profile> findByOrganizationIdAndId(UUID organizationId, UUID id);
    List<Profile> findByOrganizationId(UUID organizationId);
}
