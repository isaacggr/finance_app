package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    List<Goal> findByUserIdOrderByPrimaryDescCreatedAtDesc(UUID userId);

    Optional<Goal> findByUserIdAndPrimaryTrue(UUID userId);

    Optional<Goal> findFirstByUserIdAndIdNotOrderByCreatedAtDesc(UUID userId, UUID excludedGoalId);
}
