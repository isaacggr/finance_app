package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionCategory;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillAlertServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BillAlertService billAlertService;

    private User user1;
    private User user2;
    private Transaction bill1;

    @BeforeEach
    void setUp() {
        user1 = new User("Isaac Gregorio", "isaac@example.com", "hash1");
        ReflectionTestUtils.setField(user1, "id", UUID.randomUUID());
        user1.verifyEmail();

        user2 = new User("Maria Silva", "maria@example.com", "hash2");
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());
        user2.verifyEmail();

        bill1 = new Transaction(
                user1,
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                TransactionCategory.OUTROS,
                LocalDate.now().plusDays(2),
                "Internet Fibra"
        );
    }

    @Test
    @DisplayName("Deve processar alertas diários e enviar e-mails apenas para usuários com contas a vencer")
    void processDailyBillAlerts_Success() {
        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
        when(transactionRepository.findUpcomingTransactions(eq(user1.getId()), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(bill1));
        when(transactionRepository.findUpcomingTransactions(eq(user2.getId()), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        billAlertService.processDailyBillAlerts();

        verify(emailService, times(1)).sendBillAlertEmail(eq(user1.getEmail()), eq(user1.getName()), eq(List.of(bill1)), eq(BillAlertService.DEFAULT_DAYS_AHEAD));
        verify(emailService, never()).sendBillAlertEmail(eq(user2.getEmail()), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Deve continuar o processamento caso um usuário lance exceção no envio de alerta")
    void processDailyBillAlerts_ContinuesProcessing_WhenOneUserFails() {
        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
        when(transactionRepository.findUpcomingTransactions(eq(user1.getId()), any(), any(), any()))
                .thenThrow(new RuntimeException("Falha na consulta"));

        Transaction bill2 = new Transaction(
                user2,
                new BigDecimal("250.00"),
                TransactionType.EXPENSE,
                TransactionCategory.ALUGUEL,
                LocalDate.now().plusDays(1),
                "Energia"
        );
        when(transactionRepository.findUpcomingTransactions(eq(user2.getId()), any(), any(), any()))
                .thenReturn(List.of(bill2));

        billAlertService.processDailyBillAlerts();

        verify(emailService, never()).sendBillAlertEmail(eq(user1.getEmail()), any(), any(), anyInt());
        verify(emailService, times(1)).sendBillAlertEmail(eq(user2.getEmail()), eq(user2.getName()), eq(List.of(bill2)), eq(BillAlertService.DEFAULT_DAYS_AHEAD));
    }

    @Test
    @DisplayName("Deve enviar alerta sob demanda com janela de dias personalizada")
    void sendAlertsForUser_Success_WithCustomDays() {
        when(transactionRepository.findUpcomingTransactions(eq(user1.getId()), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(bill1));

        int count = billAlertService.sendAlertsForUser(user1, 7);

        assertEquals(1, count);
        verify(emailService, times(1)).sendBillAlertEmail(eq(user1.getEmail()), eq(user1.getName()), eq(List.of(bill1)), eq(7));
    }

    @Test
    @DisplayName("Deve aplicar janela padrão quando daysAhead for nulo ou inválido")
    void sendAlertsForUser_DefaultDays_WhenNullOrInvalid() {
        when(transactionRepository.findUpcomingTransactions(eq(user1.getId()), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        int countNull = billAlertService.sendAlertsForUser(user1, null);
        int countNegative = billAlertService.sendAlertsForUser(user1, -1);
        int countExceeded = billAlertService.sendAlertsForUser(user1, 35);

        assertEquals(0, countNull);
        assertEquals(0, countNegative);
        assertEquals(0, countExceeded);
        verify(emailService, never()).sendBillAlertEmail(any(), any(), any(), anyInt());
    }
}
