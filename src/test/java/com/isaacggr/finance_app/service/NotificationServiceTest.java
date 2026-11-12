package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.entity.Notification;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.NotificationType;
import com.isaacggr.finance_app.dto.NotificationReadAllResponseDTO;
import com.isaacggr.finance_app.dto.NotificationResponseDTO;
import com.isaacggr.finance_app.dto.PageResponseDTO;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(user, "id", userId);
        lenient().when(preferenceService.getEntityOrDefault(user)).thenReturn(UserNotificationPreference.defaultFor(user));
    }

    @Test
    @DisplayName("Deve criar notificação de vencimento hoje e evitar duplicação com mesma dedupKey")
    void notifyBillDueAlert_IdempotentByDedupKey() {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);
        Bill bill = new Bill(user, "Internet Fibra", null, "MORADIA", new BigDecimal("120.00"), today, BillRecurrence.MONTHLY);
        UUID billId = UUID.randomUUID();
        ReflectionTestUtils.setField(bill, "id", billId);

        String expectedKey = "BILL_DUE_TODAY:" + today + ":" + billId;
        when(notificationRepository.existsByUserIdAndDedupKey(userId, expectedKey))
                .thenReturn(false)
                .thenReturn(true);

        notificationService.notifyBillDueAlert(user, bill, 0);
        notificationService.notifyBillDueAlert(user, bill, 0);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.BILL_DUE_TODAY, saved.getType());
        assertEquals("BILL", saved.getReferenceType());
        assertEquals(billId, saved.getReferenceId());
        assertEquals(expectedKey, saved.getDedupKey());
        assertFalse(saved.isRead());
    }

    @Test
    @DisplayName("Não deve criar notificação de meta concluída quando usuário desativar goalAlertsInAppEnabled")
    void notifyGoalCompleted_RespectsPreference() {
        UserNotificationPreference disabled = new UserNotificationPreference(
                user, true, true, 3, true, true, true, false
        );
        when(preferenceService.getEntityOrDefault(user)).thenReturn(disabled);

        Goal goal = new Goal(user, "Reserva", null, new BigDecimal("1000.00"), null, true);
        ReflectionTestUtils.setField(goal, "id", UUID.randomUUID());

        notificationService.notifyGoalCompleted(user, goal);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar apenas não lidas quando unreadOnly=true e marcar como lida individualmente ou em lote")
    void listMarkReadAndDelete_Workflow() {
        UUID notifId = UUID.randomUUID();
        Notification n = new Notification(user, NotificationType.GOAL_COMPLETED, "Meta concluída", "Parabéns!", "GOAL", UUID.randomUUID(), "GOAL_1");
        ReflectionTestUtils.setField(n, "id", notifId);

        when(notificationRepository.findByUserIdAndReadFalse(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.of(n));
        when(notificationRepository.save(n)).thenReturn(n);
        when(notificationRepository.markAllAsReadByUserId(eq(userId), any())).thenReturn(3);

        PageResponseDTO<NotificationResponseDTO> page = notificationService.listNotifications(userId, true, PageRequest.of(0, 10));
        assertEquals(1, page.content().size());
        assertFalse(page.content().getFirst().read());

        NotificationResponseDTO readDto = notificationService.markAsRead(notifId, userId);
        assertTrue(readDto.read());
        assertNotNull(readDto.readAt());

        NotificationReadAllResponseDTO allDto = notificationService.markAllAsRead(userId);
        assertEquals(3, allDto.updatedCount());

        notificationService.delete(notifId, userId);
        verify(notificationRepository).delete(n);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao tentar ler ou excluir notificação de outro usuário")
    void markAsRead_ThrowsNotFoundForNonOwned() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.markAsRead(notifId, userId));
        assertThrows(NotFoundException.class, () -> notificationService.delete(notifId, userId));
    }
}
