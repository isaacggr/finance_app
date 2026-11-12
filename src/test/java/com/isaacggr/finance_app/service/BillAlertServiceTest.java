package com.isaacggr.finance_app.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillAlertServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BillAlertService billAlertService;

    private User user1;
    private User user2;
    private Bill bill1;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(BillAlertService.DEFAULT_ZONE);

        user1 = new User("Isaac Gregorio", "isaac@example.com", "hash1");
        ReflectionTestUtils.setField(user1, "id", UUID.randomUUID());
        user1.verifyEmail();

        user2 = new User("Maria Silva", "maria@example.com", "hash2");
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());
        user2.verifyEmail();

        bill1 = new Bill(
                user1,
                "Internet Fibra",
                "Fatura mensal",
                "moradia",
                new BigDecimal("120.00"),
                today.plusDays(3),
                BillRecurrence.MONTHLY
        );

        lenient().when(preferenceService.getEntityOrDefault(any()))
                .thenAnswer(inv -> UserNotificationPreference.defaultFor(inv.getArgument(0)));
    }

    @Test
    @DisplayName("Deve alertar no CRON apenas contas pendentes que vencem hoje ou em exatamente 3 dias")
    @SuppressWarnings("unchecked")
    void processDailyBillAlerts_Success_NotifiesOnlyDueTodayOrThreeDaysAhead() {
        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
        when(billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                eq(user1.getId()), eq(BillStatus.PENDING), anyCollection()))
                .thenReturn(List.of(bill1));
        when(billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                eq(user2.getId()), eq(BillStatus.PENDING), anyCollection()))
                .thenReturn(Collections.emptyList());

        billAlertService.processDailyBillAlerts();

        ArgumentCaptor<Collection<LocalDate>> datesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(billRepository).findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                eq(user1.getId()), eq(BillStatus.PENDING), datesCaptor.capture());

        Collection<LocalDate> capturedDates = datesCaptor.getValue();
        assertEquals(2, capturedDates.size());
        assertTrue(capturedDates.contains(today));
        assertTrue(capturedDates.contains(today.plusDays(3)));

        verify(emailService, times(1)).sendBillAlertEmail(
                eq(user1.getEmail()), eq(user1.getName()), eq(List.of(bill1)), eq(BillAlertService.DEFAULT_DAYS_AHEAD));
        verify(emailService, never()).sendBillAlertEmail(eq(user2.getEmail()), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Deve continuar o processamento caso um usuário lance exceção no envio de alerta sem vazar e-mail nos logs")
    void processDailyBillAlerts_ContinuesProcessing_WhenOneUserFails() {
        Logger logger = (Logger) LoggerFactory.getLogger(BillAlertService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
            when(billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                    eq(user1.getId()), eq(BillStatus.PENDING), anyCollection()))
                    .thenThrow(new RuntimeException("Falha na consulta"));

            Bill bill2 = new Bill(
                    user2,
                    "Energia",
                    null,
                    "MORADIA",
                    new BigDecimal("250.00"),
                    today,
                    BillRecurrence.ONCE
            );
            when(billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                    eq(user2.getId()), eq(BillStatus.PENDING), anyCollection()))
                    .thenReturn(List.of(bill2));

            billAlertService.processDailyBillAlerts();

            verify(emailService, never()).sendBillAlertEmail(eq(user1.getEmail()), any(), any(), anyInt());
            verify(emailService, times(1)).sendBillAlertEmail(
                    eq(user2.getEmail()), eq(user2.getName()), eq(List.of(bill2)), eq(BillAlertService.DEFAULT_DAYS_AHEAD));

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (a, b) -> a + "\n" + b);
            assertTrue(logs.contains(user1.getId().toString()));
            assertTrue(!logs.contains(user1.getEmail()), "E-mail do usuário não deve aparecer no log de erro");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("Deve enviar alerta sob demanda com janela de dias personalizada")
    void sendAlertsForUser_Success_WithCustomDays() {
        when(billRepository.findOpenBillsUpTo(user1.getId(), today.plusDays(7)))
                .thenReturn(List.of(bill1));

        int count = billAlertService.sendAlertsForUser(user1, 7);

        assertEquals(1, count);
        verify(emailService, times(1)).sendBillAlertEmail(
                eq(user1.getEmail()), eq(user1.getName()), eq(List.of(bill1)), eq(7));
    }

    @Test
    @DisplayName("Deve aplicar janela padrão quando daysAhead for nulo ou inválido")
    void sendAlertsForUser_DefaultDays_WhenNullOrInvalid() {
        when(billRepository.findOpenBillsUpTo(user1.getId(), today.plusDays(BillAlertService.DEFAULT_DAYS_AHEAD)))
                .thenReturn(Collections.emptyList());

        int countNull = billAlertService.sendAlertsForUser(user1, null);
        int countNegative = billAlertService.sendAlertsForUser(user1, -1);
        int countExceeded = billAlertService.sendAlertsForUser(user1, 35);

        assertEquals(0, countNull);
        assertEquals(0, countNegative);
        assertEquals(0, countExceeded);
        verify(billRepository, times(3))
                .findOpenBillsUpTo(user1.getId(), today.plusDays(BillAlertService.DEFAULT_DAYS_AHEAD));
        verify(emailService, never()).sendBillAlertEmail(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Deve respeitar preferência do usuário no CRON (e-mail desabilitado, dias customizados e sino habilitado)")
    void processDailyBillAlerts_RespectsUserPreferencesAndInAppAlerts() {
        UserNotificationPreference customPrefs = new UserNotificationPreference(
                user1, false, true, 5, true, true, true, true
        );
        when(preferenceService.getEntityOrDefault(user1)).thenReturn(customPrefs);

        Bill dueIn5Days = new Bill(user1, "Condomínio", null, "MORADIA", new BigDecimal("450.00"), today.plusDays(5), BillRecurrence.MONTHLY);
        ReflectionTestUtils.setField(dueIn5Days, "id", UUID.randomUUID());

        Bill overdue = new Bill(user1, "Água", null, "MORADIA", new BigDecimal("85.00"), today.minusDays(1), BillRecurrence.MONTHLY);
        ReflectionTestUtils.setField(overdue, "id", UUID.randomUUID());

        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1));
        when(billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                eq(user1.getId()), eq(BillStatus.PENDING), eq(List.of(today, today.plusDays(5)))))
                .thenReturn(List.of(dueIn5Days));
        when(billRepository.findOverdueByUserId(user1.getId(), today))
                .thenReturn(List.of(overdue));

        billAlertService.processDailyBillAlerts();

        verify(emailService, never()).sendBillAlertEmail(any(), any(), any(), anyInt());
        verify(notificationService).notifyBillDueAlert(user1, dueIn5Days, 5L);
        verify(notificationService).notifyBillDueAlert(user1, overdue, -1L);
    }
}
