package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.entity.GoalContribution;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.goal.GoalContributionCreateDTO;
import com.isaacggr.finance_app.dto.goal.GoalContributionResponseDTO;
import com.isaacggr.finance_app.dto.goal.GoalCreateDTO;
import com.isaacggr.finance_app.dto.goal.GoalResponseDTO;
import com.isaacggr.finance_app.dto.goal.GoalUpdateDTO;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.GoalContributionRepository;
import com.isaacggr.finance_app.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final NotificationService notificationService;

    public GoalService(
            GoalRepository goalRepository,
            GoalContributionRepository goalContributionRepository,
            NotificationService notificationService
    ) {
        this.goalRepository = goalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.notificationService = notificationService;
    }

    /**
     * Cria nova meta financeira; por padrão já define a recém-criada como principal (primary = true),
     * desmarcando a anterior na mesma transação. Se houver saldo inicial (> 0), registra o 1º aporte.
     */
    public GoalResponseDTO create(User user, GoalCreateDTO dto) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        boolean shouldBePrimary = (dto.primary() == null) || dto.primary();
        if (shouldBePrimary) {
            clearCurrentPrimaryGoal(user.getId(), null);
        }

        Goal goal = new Goal(
                user,
                dto.title(),
                dto.description(),
                dto.targetAmount(),
                dto.deadline(),
                shouldBePrimary
        );
        Goal savedGoal = goalRepository.save(goal);

        if (dto.initialAmount() != null && dto.initialAmount().compareTo(BigDecimal.ZERO) > 0) {
            savedGoal.applyContribution(dto.initialAmount());
            goalContributionRepository.save(
                    new GoalContribution(savedGoal, dto.initialAmount(), null, "Aporte inicial")
            );
            savedGoal = goalRepository.save(savedGoal);
        }

        if (savedGoal.isCompleted()) {
            notificationService.notifyGoalCompleted(savedGoal.getUser(), savedGoal);
        }

        return GoalResponseDTO.fromEntity(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> listAll(UUID userId) {
        return goalRepository.findByUserIdOrderByPrimaryDescCreatedAtDesc(userId)
                .stream()
                .map(GoalResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<GoalResponseDTO> findPrimary(UUID userId) {
        return goalRepository.findByUserIdAndPrimaryTrue(userId)
                .map(GoalResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public GoalResponseDTO findById(UUID goalId, UUID userId) {
        return GoalResponseDTO.fromEntity(findOwnedGoal(goalId, userId));
    }

    public GoalResponseDTO update(UUID goalId, UUID userId, GoalUpdateDTO dto) {
        Goal goal = findOwnedGoal(goalId, userId);

        boolean targetPrimary = (dto.primary() != null) ? dto.primary() : goal.isPrimary();
        if (targetPrimary && !goal.isPrimary()) {
            clearCurrentPrimaryGoal(userId, goalId);
        }

        goal.update(dto.title(), dto.description(), dto.targetAmount(), dto.deadline(), targetPrimary);
        Goal saved = goalRepository.save(goal);
        if (saved.isCompleted()) {
            notificationService.notifyGoalCompleted(saved.getUser(), saved);
        }
        return GoalResponseDTO.fromEntity(saved);
    }

    public GoalResponseDTO setAsPrimary(UUID goalId, UUID userId) {
        Goal goal = findOwnedGoal(goalId, userId);

        if (!goal.isPrimary()) {
            clearCurrentPrimaryGoal(userId, goalId);
            goal.markAsPrimary();
            goal = goalRepository.save(goal);
        }

        return GoalResponseDTO.fromEntity(goal);
    }

    public void delete(UUID goalId, UUID userId) {
        Goal goal = findOwnedGoal(goalId, userId);
        boolean wasPrimary = goal.isPrimary();
        goalRepository.delete(goal);

        if (wasPrimary) {
            goalRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, goalId)
                    .ifPresent(nextPrimary -> {
                        nextPrimary.markAsPrimary();
                        goalRepository.save(nextPrimary);
                    });
        }
    }

    public GoalContributionResponseDTO addContribution(UUID goalId, UUID userId, GoalContributionCreateDTO dto) {
        Goal goal = findOwnedGoal(goalId, userId);

        GoalContribution contribution = new GoalContribution(
                goal,
                dto.amount(),
                dto.contributionDate(),
                dto.note()
        );
        goal.applyContribution(contribution.getAmount());

        GoalContribution savedContribution = goalContributionRepository.save(contribution);
        Goal savedGoal = goalRepository.save(goal);

        if (savedGoal.isCompleted()) {
            notificationService.notifyGoalCompleted(savedGoal.getUser(), savedGoal);
        }

        return GoalContributionResponseDTO.fromEntityWithGoal(savedContribution, savedGoal);
    }

    @Transactional(readOnly = true)
    public List<GoalContributionResponseDTO> listContributions(UUID goalId, UUID userId) {
        findOwnedGoal(goalId, userId);
        return goalContributionRepository.findByGoalIdOrderByContributionDateDescCreatedAtDesc(goalId)
                .stream()
                .map(GoalContributionResponseDTO::fromEntity)
                .toList();
    }

    public void deleteContribution(UUID goalId, UUID contributionId, UUID userId) {
        GoalContribution contribution = goalContributionRepository
                .findByIdAndGoalIdAndGoalUserId(contributionId, goalId, userId)
                .orElseThrow(() -> new NotFoundException("Aporte não encontrado"));

        Goal goal = contribution.getGoal();
        goal.revertContribution(contribution.getAmount());

        goalContributionRepository.delete(contribution);
        goalRepository.save(goal);
    }

    private Goal findOwnedGoal(UUID goalId, UUID userId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Meta financeira não encontrada"));
    }

    private void clearCurrentPrimaryGoal(UUID userId, UUID excludeGoalId) {
        goalRepository.findByUserIdAndPrimaryTrue(userId)
                .filter(currentPrimary -> excludeGoalId == null || !excludeGoalId.equals(currentPrimary.getId()))
                .ifPresent(currentPrimary -> {
                    currentPrimary.unmarkAsPrimary();
                    goalRepository.save(currentPrimary);
                });
    }
}
