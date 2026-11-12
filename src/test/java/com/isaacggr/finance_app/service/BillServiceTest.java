package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.BillCreateDTO;
import com.isaacggr.finance_app.dto.BillPayRequestDTO;
import com.isaacggr.finance_app.dto.BillPayResponseDTO;
import com.isaacggr.finance_app.dto.BillResponseDTO;
import com.isaacggr.finance_app.dto.BillUpdateDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.BillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private BillService billService;

    private User sampleUser;
    private UUID userId;
    private UUID billId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(Bill.DEFAULT_ZONE);
        userId = UUID.randomUUID();
        billId = UUID.randomUUID();

        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(sampleUser, "id", userId);
        sampleUser.verifyEmail();
    }

    @Test
    @DisplayName("Deve criar conta normalizando valor, categoria e assumindo ONCE quando recorrência é omitida")
    void create_Success_NormalizesFieldsAndDefaultsRecurrence() {
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", billId);
            return saved;
        });

        BillCreateDTO dto = new BillCreateDTO(
                "  Internet Fibra  ",
                "   ",
                " moradia ",
                new BigDecimal("129.9"),
                today.plusDays(3),
                null
        );

        BillResponseDTO response = billService.create(sampleUser, dto);

        assertEquals(billId, response.id());
        assertEquals("Internet Fibra", response.title());
        assertNull(response.description());
        assertEquals("MORADIA", response.category());
        assertEquals(new BigDecimal("129.90"), response.amount());
        assertEquals(3L, response.dueInDays());
        assertEquals(BillRecurrence.ONCE, response.recurrence());
        assertEquals(BillStatus.PENDING, response.status());
    }

    @Test
    @DisplayName("Deve derivar status OVERDUE e dueInDays negativo quando vencimento for anterior a hoje")
    void listAll_OverdueFilter_ReturnsDerivedOverdueStatus() {
        Bill overdueBill = new Bill(
                sampleUser,
                "Energia",
                null,
                "MORADIA",
                new BigDecimal("210.00"),
                today.minusDays(2),
                BillRecurrence.MONTHLY
        );
        ReflectionTestUtils.setField(overdueBill, "id", billId);

        when(billRepository.findOverdueByUserId(userId, today)).thenReturn(List.of(overdueBill));

        List<BillResponseDTO> result = billService.listAll(userId, BillStatus.OVERDUE);

        assertEquals(1, result.size());
        assertEquals(BillStatus.OVERDUE, result.getFirst().status());
        assertEquals(-2L, result.getFirst().dueInDays());
    }

    @Test
    @DisplayName("Deve listar contas em aberto na janela padrão de 7 dias e rejeitar janelas inválidas")
    void listUpcoming_AppliesDefaultAndRejectsOutOfRangeDays() {
        when(billRepository.findOpenBillsUpTo(userId, today.plusDays(BillService.DEFAULT_UPCOMING_DAYS)))
                .thenReturn(List.of());

        List<BillResponseDTO> result = billService.listUpcoming(userId, null);

        assertTrue(result.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> billService.listUpcoming(userId, 0));
        assertThrows(IllegalArgumentException.class, () -> billService.listUpcoming(userId, 91));
    }

    @Test
    @DisplayName("Deve rejeitar edição de conta já paga com ConflictException")
    void update_PaidBill_ThrowsConflictException() {
        Bill paidBill = new Bill(sampleUser, "Água", null, "MORADIA", new BigDecimal("80.00"), today, BillRecurrence.ONCE);
        ReflectionTestUtils.setField(paidBill, "id", billId);
        paidBill.markAsPaid(today, null);

        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(paidBill));

        BillUpdateDTO dto = new BillUpdateDTO("Água Atualizada", null, "MORADIA", new BigDecimal("90.00"), today.plusDays(2), BillRecurrence.ONCE);

        assertThrows(ConflictException.class, () -> billService.update(billId, userId, dto));
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve pagar conta única gerando despesa em /transactions por padrão")
    void pay_OnceBill_CreatesExpenseTransactionByDefault() {
        Bill pendingBill = new Bill(
                sampleUser,
                "Cartão Nubank",
                "Fatura novembro",
                "CARTAO",
                new BigDecimal("950.00"),
                today.plusDays(1),
                BillRecurrence.ONCE
        );
        ReflectionTestUtils.setField(pendingBill, "id", billId);

        UUID txId = UUID.randomUUID();
        Transaction generatedTx = new Transaction(
                sampleUser,
                new BigDecimal("950.00"),
                TransactionType.EXPENSE,
                "CARTAO",
                today,
                "Pagamento: Cartão Nubank"
        );
        ReflectionTestUtils.setField(generatedTx, "id", txId);

        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(pendingBill));
        when(transactionService.create(
                sampleUser,
                new BigDecimal("950.00"),
                TransactionType.EXPENSE,
                "CARTAO",
                today,
                "Pagamento: Cartão Nubank"
        )).thenReturn(generatedTx);
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillPayResponseDTO response = billService.pay(billId, sampleUser, null);

        assertEquals(BillStatus.PAID, response.bill().status());
        assertEquals(today, response.bill().paidAt());
        assertEquals(txId, response.bill().paidTransactionId());
        assertEquals(txId, response.generatedTransactionId());
        assertNull(response.nextBill());
    }

    @Test
    @DisplayName("Deve pagar conta sem gerar transação quando createTransaction for false")
    void pay_WithCreateTransactionFalse_DoesNotCreateTransaction() {
        Bill pendingBill = new Bill(
                sampleUser,
                "Seguro",
                null,
                "TRANSPORTE",
                new BigDecimal("400.00"),
                today,
                BillRecurrence.ONCE
        );
        ReflectionTestUtils.setField(pendingBill, "id", billId);

        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(pendingBill));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate customDate = today.minusDays(1);
        BillPayResponseDTO response = billService.pay(billId, sampleUser, new BillPayRequestDTO(false, customDate));

        verifyNoInteractions(transactionService);
        assertEquals(BillStatus.PAID, response.bill().status());
        assertEquals(customDate, response.bill().paidAt());
        assertNull(response.generatedTransactionId());
        assertNull(response.bill().paidTransactionId());
    }

    @Test
    @DisplayName("Deve agendar próxima conta PENDING automaticamente ao pagar conta MONTHLY ou YEARLY")
    void pay_RecurringMonthlyBill_CreatesNextMonthBill() {
        LocalDate originalDueDate = LocalDate.of(2026, 11, 15);
        Bill monthlyBill = new Bill(
                sampleUser,
                "Aluguel",
                "Apartamento",
                "MORADIA",
                new BigDecimal("1800.00"),
                originalDueDate,
                BillRecurrence.MONTHLY
        );
        ReflectionTestUtils.setField(monthlyBill, "id", billId);

        UUID nextBillId = UUID.randomUUID();
        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(monthlyBill));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", nextBillId);
            }
            return saved;
        });

        BillPayResponseDTO response = billService.pay(billId, sampleUser, new BillPayRequestDTO(false, today));

        assertEquals(BillStatus.PAID, response.bill().status());
        assertNotNull(response.nextBill());
        assertEquals(nextBillId, response.nextBill().id());
        assertEquals(LocalDate.of(2026, 12, 15), response.nextBill().dueDate());
        assertEquals(BillStatus.PENDING, response.nextBill().status());
        assertEquals(BillRecurrence.MONTHLY, response.nextBill().recurrence());

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(2)).save(captor.capture());
    }

    @Test
    @DisplayName("Deve impedir pagamento duplicado de conta já paga com ConflictException")
    void pay_AlreadyPaidBill_ThrowsConflictException() {
        Bill paidBill = new Bill(sampleUser, "Academia", null, "SAUDE", new BigDecimal("99.90"), today, BillRecurrence.MONTHLY);
        ReflectionTestUtils.setField(paidBill, "id", billId);
        paidBill.markAsPaid(today, null);

        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(paidBill));

        assertThrows(ConflictException.class, () -> billService.pay(billId, sampleUser, null));
        verifyNoInteractions(transactionService);
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve excluir conta pertencente ao usuário e lançar NotFoundException quando inexistente")
    void delete_Success_AndThrowsWhenNotFound() {
        Bill existing = new Bill(sampleUser, "Streaming", null, "LAZER", new BigDecimal("45.00"), today.plusDays(4), BillRecurrence.MONTHLY);
        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(existing));

        billService.delete(billId, userId);
        verify(billRepository).delete(existing);

        UUID otherBillId = UUID.randomUUID();
        when(billRepository.findByIdAndUserId(otherBillId, userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> billService.delete(otherBillId, userId));
    }
}
