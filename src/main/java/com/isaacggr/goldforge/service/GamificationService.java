package com.isaacggr.goldforge.service;

import com.isaacggr.goldforge.domain.entity.User;
import com.isaacggr.goldforge.domain.entity.UserGameProfile;
import com.isaacggr.goldforge.domain.entity.UserQuest;
import com.isaacggr.goldforge.domain.enums.QuestCategory;
import com.isaacggr.goldforge.domain.enums.QuestStatus;
import com.isaacggr.goldforge.dto.gamification.ClaimQuestResponseDTO;
import com.isaacggr.goldforge.dto.gamification.GameProfileResponseDTO;
import com.isaacggr.goldforge.dto.gamification.QuestResponseDTO;
import com.isaacggr.goldforge.exception.NotFoundException;
import com.isaacggr.goldforge.repository.TransactionRepository;
import com.isaacggr.goldforge.repository.UserGameProfileRepository;
import com.isaacggr.goldforge.repository.UserQuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    public static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    public static final int DAILY_CHECKIN_XP = 15;

    private final UserGameProfileRepository gameProfileRepository;
    private final UserQuestRepository questRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public UserGameProfile getOrCreateProfile(User user) {
        return gameProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> gameProfileRepository.save(new UserGameProfile(user)));
    }

    @Transactional(readOnly = true)
    public GameProfileResponseDTO getProfile(User user) {
        UserGameProfile profile = getOrCreateProfile(user);
        LocalDate today = LocalDate.now(ZONE_SP);
        return GameProfileResponseDTO.fromEntity(profile, today);
    }

    /**
     * Realiza o check-in diário do jogador.
     * Concede XP pelo hábito de consciência financeira e atualiza a missão de check-in.
     */
    @Transactional
    public ClaimQuestResponseDTO performDailyCheckin(User user) {
        UserGameProfile profile = getOrCreateProfile(user);
        LocalDate today = LocalDate.now(ZONE_SP);

        boolean newCheckin = profile.recordCheckin(today);
        int xpGained = 0;
        boolean leveledUp = false;

        ensureActiveQuests(user);

        if (newCheckin) {
            xpGained = DAILY_CHECKIN_XP;
            leveledUp = profile.addXp(xpGained);
            gameProfileRepository.save(profile);

            // Marca a quest diária de check-in como concluída/resgatada
            String dailyPeriod = today.toString();
            questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "FINANCIAL_CHECKIN", dailyPeriod)
                    .ifPresent(quest -> {
                        quest.incrementProgress(1);
                        quest.claim();
                        questRepository.save(quest);
                    });
        }

        String message = leveledUp
                ? String.format("Check-in realizado! Você ganhou +%d XP e subiu para o Nível %d! 🆙", xpGained, profile.getCurrentLevel())
                : (newCheckin
                    ? String.format("Check-in realizado com sucesso! +%d XP por cuidar das suas finanças hoje. 🔥 Streak: %d dias!", xpGained, profile.getStreakDays())
                    : "Você já realizou o check-in diário hoje. Volte amanhã para continuar sua sequência!");

        return new ClaimQuestResponseDTO(
                null,
                xpGained,
                profile.getTotalXp(),
                profile.getCurrentLevel(),
                leveledUp,
                message
        );
    }

    /**
     * Retorna a lista de todas as missões ativas e recentes do usuário.
     */
    @Transactional
    public List<QuestResponseDTO> getQuests(User user) {
        ensureActiveQuests(user);
        evaluateAutomaticQuests(user);

        return questRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(QuestResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Resgata a recompensa em XP de uma missão concluída.
     */
    @Transactional
    public ClaimQuestResponseDTO claimQuest(User user, UUID questId) {
        UserQuest quest = questRepository.findByIdAndUserId(questId, user.getId())
                .orElseThrow(() -> new NotFoundException("Missão não encontrada."));

        int xpReward = quest.claim();
        UserGameProfile profile = getOrCreateProfile(user);
        boolean leveledUp = profile.addXp(xpReward);

        questRepository.save(quest);
        gameProfileRepository.save(profile);

        String message = leveledUp
                ? String.format("Incrível! Você resgatou +%d XP da missão '%s' e SUBIU PARA O NÍVEL %d! 🏆", xpReward, quest.getTitle(), profile.getCurrentLevel())
                : String.format("Recompensa resgatada! +%d XP da missão '%s'.", xpReward, quest.getTitle());

        return new ClaimQuestResponseDTO(
                quest.getId(),
                xpReward,
                profile.getTotalXp(),
                profile.getCurrentLevel(),
                leveledUp,
                message
        );
    }

    /**
     * Garante que o usuário possua as missões diárias, semanais e marcos iniciais criados.
     */
    @Transactional
    public void ensureActiveQuests(User user) {
        LocalDate today = LocalDate.now(ZONE_SP);
        String dailyKey = today.toString();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = today.get(WeekFields.ISO.weekBasedYear());
        String weeklyKey = String.format("%d-W%02d", year, weekNumber);
        LocalDate endOfCurrentWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDateTime endOfWeek = endOfCurrentWeek.atTime(LocalTime.MAX);

        List<UserQuest> toCreate = new ArrayList<>();

        // 1. Quests Diárias
        addQuestIfAbsent(toCreate, user, "FINANCIAL_CHECKIN", QuestCategory.DAILY,
                "Check-in de Consciência", "Abra o FinanGame para avaliar seu saldo e próximos vencimentos.",
                15, 1, dailyKey, endOfDay);

        addQuestIfAbsent(toCreate, user, "NO_SPEND_DAY", QuestCategory.DAILY,
                "Escudo de Gasto Zero", "Passe o dia sem despesas registradas e proteja seu dinheiro.",
                30, 1, dailyKey, endOfDay);

        // 2. Quests Semanais
        addQuestIfAbsent(toCreate, user, "GOAL_SAVINGS", QuestCategory.WEEKLY,
                "Aporte no Tesouro", "Faça ao menos um aporte em qualquer meta financeira durante a semana.",
                60, 1, weeklyKey, endOfWeek);

        addQuestIfAbsent(toCreate, user, "BILLS_ON_TIME", QuestCategory.WEEKLY,
                "Pontualidade Heroica", "Pague uma conta a vencer antes ou no dia do vencimento.",
                50, 1, weeklyKey, endOfWeek);

        // 3. Conquistas Permanentes (LIFETIME)
        addQuestIfAbsent(toCreate, user, "FIRST_GOAL_COMPLETED", QuestCategory.MILESTONE,
                "Primeira Meta Concluída", "Complete 100% de uma meta financeira e celebre sua conquista!",
                200, 1, "LIFETIME", null);

        addQuestIfAbsent(toCreate, user, "FIRST_500_SAVED", QuestCategory.MILESTONE,
                "Guardião dos R$ 500", "Acumule pelo menos R$ 500,00 guardados em suas metas.",
                150, 1, "LIFETIME", null);

        if (!toCreate.isEmpty()) {
            questRepository.saveAll(toCreate);
        }
    }

    private void addQuestIfAbsent(
            List<UserQuest> list,
            User user,
            String code,
            QuestCategory category,
            String title,
            String desc,
            int xp,
            int target,
            String periodKey,
            LocalDateTime expiresAt
    ) {
        Optional<UserQuest> existing = questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), code, periodKey);
        if (existing.isEmpty()) {
            list.add(new UserQuest(user, code, category, title, desc, xp, target, periodKey, expiresAt));
        }
    }

    /**
     * Avalia o progresso automático de missões baseado no estado financeiro real do usuário.
     */
    @Transactional
    public void evaluateAutomaticQuests(User user) {
        LocalDate today = LocalDate.now(ZONE_SP);
        String dailyKey = today.toString();

        // 1. Avalia o Dia de Gasto Zero (se não houver despesas registradas com data de hoje)
        questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "NO_SPEND_DAY", dailyKey)
                .filter(q -> q.getStatus() == QuestStatus.IN_PROGRESS)
                .ifPresent(quest -> {
                    boolean hasExpensesToday = transactionRepository.existsByUserIdAndDateAndType(
                            user.getId(), today, com.isaacggr.goldforge.domain.enums.TransactionType.EXPENSE);
                    if (!hasExpensesToday) {
                        quest.incrementProgress(1);
                        questRepository.save(quest);
                    }
                });
    }

    /**
     * Gatilho disparado quando o usuário realiza um aporte em uma Meta.
     */
    @Transactional
    public void onGoalContribution(User user, BigDecimal totalAmountInGoal, boolean isGoalFullyCompleted) {
        LocalDate today = LocalDate.now(ZONE_SP);
        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = today.get(WeekFields.ISO.weekBasedYear());
        String weeklyKey = String.format("%d-W%02d", year, weekNumber);

        // Avança a quest semanal de aporte
        questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "GOAL_SAVINGS", weeklyKey)
                .ifPresent(quest -> {
                    quest.incrementProgress(1);
                    questRepository.save(quest);
                });

        // Avalia marco de R$ 500 poupados
        if (totalAmountInGoal != null && totalAmountInGoal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "FIRST_500_SAVED", "LIFETIME")
                    .ifPresent(quest -> {
                        quest.incrementProgress(1);
                        questRepository.save(quest);
                    });
        }

        // Se a meta foi 100% concluída, avança a conquista de meta concluída!
        if (isGoalFullyCompleted) {
            questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "FIRST_GOAL_COMPLETED", "LIFETIME")
                    .ifPresent(quest -> {
                        quest.incrementProgress(1);
                        questRepository.save(quest);
                    });
        }
    }

    /**
     * Gatilho disparado quando o usuário paga uma conta no prazo.
     */
    @Transactional
    public void onBillPaid(User user, boolean onTime) {
        if (!onTime) return;
        LocalDate today = LocalDate.now(ZONE_SP);
        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = today.get(WeekFields.ISO.weekBasedYear());
        String weeklyKey = String.format("%d-W%02d", year, weekNumber);

        questRepository.findByUserIdAndQuestCodeAndPeriodKey(user.getId(), "BILLS_ON_TIME", weeklyKey)
                .ifPresent(quest -> {
                    quest.incrementProgress(1);
                    questRepository.save(quest);
                });
    }
}
