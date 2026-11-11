package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    List<GoalContribution> findByGoalIdOrderByContributionDateDescCreatedAtDesc(UUID goalId);

    Optional<GoalContribution> findByIdAndGoalIdAndGoalUserId(UUID id, UUID goalId, UUID userId);
}
