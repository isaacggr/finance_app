package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserGameProfile;
import com.isaacggr.finance_app.domain.entity.UserQuest;
import com.isaacggr.finance_app.domain.enums.QuestCategory;
import com.isaacggr.finance_app.domain.enums.QuestStatus;
import com.isaacggr.finance_app.dto.gamification.ClaimQuestResponseDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserGameProfileRepository;
import com.isaacggr.finance_app.repository.UserQuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserGameProfileRepository gameProfileRepository;

    @Mock
    private UserQuestRepository questRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private User sampleUser;
    private UserGameProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Isaac", "isaac@email.com", "hash123");
        sampleUser.verifyEmail();
        ReflectionTestUtils.setField(sampleUser, "id", UUID.randomUUID());

        sampleProfile = new UserGameProfile(sampleUser);
        ReflectionTestUtils.setField(sampleProfile, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Deve calcular corretamente a curva de níveis e porcentagem de XP")
    void shouldCalculateLevelAndProgressCorrectly() {
        // Nível 1: 0 XP
        assertEquals(1, UserGameProfile.calculateLevelForXp(0));
        assertEquals(1, UserGameProfile.calculateLevelForXp(99));

        // Nível 2: 100 XP
        assertEquals(2, UserGameProfile.calculateLevelForXp(100));
        assertEquals(2, UserGameProfile.calculateLevelForXp(250));

        // Nível 3: 300 XP
        assertEquals(3, UserGameProfile.calculateLevelForXp(300));

        // Progresso do sampleProfile
        sampleProfile.addXp(250);
        assertEquals(2, sampleProfile.getCurrentLevel());
        assertEquals(100, sampleProfile.getCurrentLevelBaseXp());
        assertEquals(300, sampleProfile.getNextLevelTargetXp());
        assertEquals(150, sampleProfile.getCurrentLevelProgressXp());
        assertEquals(200, sampleProfile.getXpRequiredForNextLevel());
        assertEquals(new BigDecimal("75.00"), sampleProfile.getProgressPercentage());
    }

    @Test
    @DisplayName("Deve realizar check-in diário concedendo XP e incrementando streak")
    void shouldPerformDailyCheckinSuccessfully() {
        when(gameProfileRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));
        when(gameProfileRepository.save(any(UserGameProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimQuestResponseDTO response = gamificationService.performDailyCheckin(sampleUser);

        assertNotNull(response);
        assertEquals(GamificationService.DAILY_CHECKIN_XP, response.xpGained());
        assertEquals(1, sampleProfile.getStreakDays());
        assertEquals(GamificationService.DAILY_CHECKIN_XP, response.newTotalXp());
        verify(gameProfileRepository, atLeastOnce()).save(sampleProfile);
    }

    @Test
    @DisplayName("Não deve conceder XP duplicado se o usuário já fez check-in hoje")
    void shouldNotAwardXpTwiceOnSameDay() {
        LocalDate today = LocalDate.now(GamificationService.ZONE_SP);
        sampleProfile.recordCheckin(today);
        sampleProfile.addXp(15);

        when(gameProfileRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));

        ClaimQuestResponseDTO response = gamificationService.performDailyCheckin(sampleUser);

        assertEquals(0, response.xpGained());
        assertEquals(15, response.newTotalXp());
        assertEquals(1, sampleProfile.getStreakDays());
        verify(gameProfileRepository, never()).save(sampleProfile);
    }

    @Test
    @DisplayName("Deve resgatar com sucesso recompensa de missão concluída e creditar XP")
    void shouldClaimCompletedQuestSuccessfully() {
        UUID questId = UUID.randomUUID();
        UserQuest quest = new UserQuest(
                sampleUser,
                "GOAL_SAVINGS",
                QuestCategory.WEEKLY,
                "Aporte no Tesouro",
                "Faça um aporte",
                60,
                1,
                "2026-W46",
                null
        );
        ReflectionTestUtils.setField(quest, "id", questId);
        quest.incrementProgress(1); // conclui a quest -> COMPLETED

        when(questRepository.findByIdAndUserId(questId, sampleUser.getId())).thenReturn(Optional.of(quest));
        when(gameProfileRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));
        when(questRepository.save(any(UserQuest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameProfileRepository.save(any(UserGameProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimQuestResponseDTO response = gamificationService.claimQuest(sampleUser, questId);

        assertEquals(60, response.xpGained());
        assertEquals(60, response.newTotalXp());
        assertEquals(QuestStatus.CLAIMED, quest.getStatus());
        verify(questRepository).save(quest);
        verify(gameProfileRepository).save(sampleProfile);
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar resgatar missão ainda em progresso")
    void shouldThrowExceptionWhenClaimingInProgressQuest() {
        UUID questId = UUID.randomUUID();
        UserQuest quest = new UserQuest(
                sampleUser,
                "BILLS_ON_TIME",
                QuestCategory.WEEKLY,
                "Pontualidade",
                "Pague no prazo",
                50,
                1,
                "2026-W46",
                null
        );
        ReflectionTestUtils.setField(quest, "id", questId);

        when(questRepository.findByIdAndUserId(questId, sampleUser.getId())).thenReturn(Optional.of(quest));

        assertThrows(BusinessException.class, () -> gamificationService.claimQuest(sampleUser, questId));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar resgatar missão inexistente ou de outro usuário (Zero IDOR)")
    void shouldThrowExceptionWhenQuestNotFoundOrDifferentUser() {
        UUID questId = UUID.randomUUID();
        when(questRepository.findByIdAndUserId(questId, sampleUser.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> gamificationService.claimQuest(sampleUser, questId));
    }

    @Test
    @DisplayName("Deve avançar quest de aporte em meta e marco de R$ 500 no gatilho onGoalContribution")
    void shouldAdvanceQuestsOnGoalContribution() {
        UserQuest goalQuest = new UserQuest(
                sampleUser, "GOAL_SAVINGS", QuestCategory.WEEKLY, "Aporte", "Desc", 60, 1, "any", null);
        UserQuest milestone500 = new UserQuest(
                sampleUser, "FIRST_500_SAVED", QuestCategory.MILESTONE, "Marco R$ 500", "Desc", 150, 1, "LIFETIME", null);
        UserQuest milestoneCompleted = new UserQuest(
                sampleUser, "FIRST_GOAL_COMPLETED", QuestCategory.MILESTONE, "Meta Concluída", "Desc", 200, 1, "LIFETIME", null);

        when(questRepository.findByUserIdAndQuestCodeAndPeriodKey(eq(sampleUser.getId()), eq("GOAL_SAVINGS"), any()))
                .thenReturn(Optional.of(goalQuest));
        when(questRepository.findByUserIdAndQuestCodeAndPeriodKey(eq(sampleUser.getId()), eq("FIRST_500_SAVED"), eq("LIFETIME")))
                .thenReturn(Optional.of(milestone500));
        when(questRepository.findByUserIdAndQuestCodeAndPeriodKey(eq(sampleUser.getId()), eq("FIRST_GOAL_COMPLETED"), eq("LIFETIME")))
                .thenReturn(Optional.of(milestoneCompleted));

        gamificationService.onGoalContribution(sampleUser, BigDecimal.valueOf(600), true);

        assertEquals(QuestStatus.COMPLETED, goalQuest.getStatus());
        assertEquals(QuestStatus.COMPLETED, milestone500.getStatus());
        assertEquals(QuestStatus.COMPLETED, milestoneCompleted.getStatus());
    }
}
