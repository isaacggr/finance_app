package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.UserGameProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGameProfileRepository extends JpaRepository<UserGameProfile, UUID> {

    Optional<UserGameProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
