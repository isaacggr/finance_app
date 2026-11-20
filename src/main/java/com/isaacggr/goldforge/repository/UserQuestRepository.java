package com.isaacggr.goldforge.repository;

import com.isaacggr.goldforge.domain.entity.UserQuest;
import com.isaacggr.goldforge.domain.enums.QuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuestRepository extends JpaRepository<UserQuest, UUID> {

    List<UserQuest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserQuest> findByUserIdAndStatus(UUID userId, QuestStatus status);

    Optional<UserQuest> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserQuest> findByUserIdAndQuestCodeAndPeriodKey(UUID userId, String questCode, String periodKey);

    List<UserQuest> findByUserIdAndPeriodKey(UUID userId, String periodKey);
}
