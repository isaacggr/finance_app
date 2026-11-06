package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.DashboardSummaryDTO;
import com.isaacggr.finance_app.exception.BusinessException;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MonthlyReportService monthlyReportService;

    private User user1;
    private User user2;
    private DashboardSummaryDTO sampleSummary;

    @BeforeEach
    void setUp() {
        user1 = new User("Isaac Gregorio", "isaac@example.com", "hash1");
        ReflectionTestUtils.setField(user1, "id", UUID.randomUUID());
        user1.verifyEmail();

        user2 = new User("Maria Silva", "maria@example.com", "hash2");
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());
        user2.verifyEmail();

        sampleSummary = new DashboardSummaryDTO(
                LocalDate.now().minusMonths(1).withDayOfMonth(1),
                LocalDate.now().minusMonths(1).withDayOfMonth(28),
                new BigDecimal("5000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("3000.00"),
                List.of(new CategoryExpenseDTO("ALIMENTACAO", new BigDecimal("800.00")))
        );
    }

    @Test
    @DisplayName("Deve processar e enviar relatórios mensais agendados para todos os usuários elegíveis")
    void processMonthlyReports_Success() {
        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
        when(dashboardService.getSummary(any(), any(), any())).thenReturn(sampleSummary);

        monthlyReportService.processMonthlyReports();

        verify(emailService, times(1)).sendMonthlyReportEmail(eq("isaac@example.com"), eq("Isaac Gregorio"), eq(sampleSummary), any());
        verify(emailService, times(1)).sendMonthlyReportEmail(eq("maria@example.com"), eq("Maria Silva"), eq(sampleSummary), any());
    }

    @Test
    @DisplayName("Deve continuar o processamento agendado mesmo se um usuário falhar")
    void processMonthlyReports_HandlesExceptionGracefully() {
        when(userRepository.findByEmailVerifiedTrueAndDeletedFalse()).thenReturn(List.of(user1, user2));
        when(dashboardService.getSummary(eq(user1.getId()), any(), any())).thenThrow(new RuntimeException("Falha no banco"));
        when(dashboardService.getSummary(eq(user2.getId()), any(), any())).thenReturn(sampleSummary);

        monthlyReportService.processMonthlyReports();

        verify(emailService, never()).sendMonthlyReportEmail(eq("isaac@example.com"), any(), any(), any());
        verify(emailService, times(1)).sendMonthlyReportEmail(eq("maria@example.com"), eq("Maria Silva"), eq(sampleSummary), any());
    }

    @Test
    @DisplayName("Deve enviar relatório sob demanda para o mês anterior quando parâmetros forem nulos")
    void sendReportForUser_Success_DefaultPreviousMonth() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String expectedPeriod = String.format("%02d/%d", lastMonth.getMonthValue(), lastMonth.getYear());

        when(dashboardService.getSummary(user1.getId(), lastMonth.atDay(1), lastMonth.atEndOfMonth()))
                .thenReturn(sampleSummary);

        monthlyReportService.sendReportForUser(user1, null, null);

        verify(emailService).sendMonthlyReportEmail(
                eq("isaac@example.com"),
                eq("Isaac Gregorio"),
                eq(sampleSummary),
                eq(expectedPeriod)
        );
    }

    @Test
    @DisplayName("Deve enviar relatório sob demanda para mês e ano específicos")
    void sendReportForUser_Success_SpecificMonthAndYear() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        when(dashboardService.getSummary(user1.getId(), start, end)).thenReturn(sampleSummary);

        monthlyReportService.sendReportForUser(user1, 10, 2026);

        verify(emailService).sendMonthlyReportEmail(
                eq("isaac@example.com"),
                eq("Isaac Gregorio"),
                eq(sampleSummary),
                eq("10/2026")
        );
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o mês for inválido (menor que 1 ou maior que 12)")
    void sendReportForUser_ThrowsBusinessException_WhenInvalidMonth() {
        BusinessException exception1 = assertThrows(
                BusinessException.class,
                () -> monthlyReportService.sendReportForUser(user1, 13, 2026)
        );
        assertEquals("O mês informado deve estar entre 1 e 12", exception1.getMessage());

        BusinessException exception2 = assertThrows(
                BusinessException.class,
                () -> monthlyReportService.sendReportForUser(user1, 0, 2026)
        );
        assertEquals("O mês informado deve estar entre 1 e 12", exception2.getMessage());

        verify(emailService, never()).sendMonthlyReportEmail(any(), any(), any(), any());
    }
}
