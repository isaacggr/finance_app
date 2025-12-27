package com.isaacggr.finance_app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isaacggr.finance_app.domain.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    List<User> findAllByDeletedFalse();
}
