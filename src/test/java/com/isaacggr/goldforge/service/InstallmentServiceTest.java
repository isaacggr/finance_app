package com.isaacggr.goldforge.service;

import com.isaacggr.goldforge.domain.entity.Bill;
import com.isaacggr.goldforge.domain.entity.InstallmentGroup;
import com.isaacggr.goldforge.domain.entity.Transaction;
import com.isaacggr.goldforge.domain.entity.User;
import com.isaacggr.goldforge.domain.enums.BillRecurrence;
import com.isaacggr.goldforge.domain.enums.BillStatus;
import com.isaacggr.goldforge.domain.enums.TransactionType;
import com.isaacggr.goldforge.dto.installment.InstallmentPurchaseCreateDTO;
import com.isaacggr.goldforge.exception.ConflictException;
import com.isaacggr.goldforge.exception.NotFoundException;
import com.isaacggr.goldforge.repository.BillRepository;
import com.isaacggr.goldforge.repository.InstallmentGroupRepository;
import com.isaacggr.goldforge.repository.TransactionRepository;
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
class InstallmentServiceTest {

    @Mock
    private InstallmentGroupRepository installmentGroupRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private InstallmentService installmentService;

    private User sampleUser;
    private UUID userId;
    private UUID groupId;
    private LocalDate startDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        startDate = LocalDate.now(Bill.DEFAULT_ZONE);

        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(sampleUser, "id", userId);
        sampleUser.verifyEmail();
    }

    @Test
    @DisplayName("Deve criar compra parcelada gerando contas a pagar (bills) com distribuição exata de centavos (ex: R$ 100 em 3x)")
    void create_Success_DistributesExactPenniesAcrossInstallments() {
        when(installmentGroupRepository.save(any(InstallmentGroup.class))).thenAnswer(inv -> {
            InstallmentGroup g = inv.getArgument(0);
            ReflectionTestUtils.setField(g, "id", groupId);
            return g;
        });

        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> {
            Bill b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
            return b;
        });

        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("100.00"),
                null,
                3,
                1,
                "  eletronicos  ",
                startDate,
                "Fone Bluetooth"
        );

        InstallmentGroup result = installmentService.create(sampleUser, dto);

        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals("ELETRONICOS", result.getCategory());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());
        assertEquals(3, result.getTotalInstallments());

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(3)).save(captor.capture());
        List<Bill> savedBills = captor.getAllValues();

        // Parcela 1: 33.34
        assertEquals(new BigDecimal("33.34"), savedBills.get(0).getAmount());
        assertEquals(1, savedBills.get(0).getInstallmentNumber());
        assertEquals(3, savedBills.get(0).getTotalInstallments());
        assertEquals("Fone Bluetooth (1/3)", savedBills.get(0).getTitle());
        assertEquals(startDate, savedBills.get(0).getDueDate());
        assertEquals(BillStatus.PENDING, savedBills.get(0).getStatus());
        assertEquals(BillRecurrence.ONCE, savedBills.get(0).getRecurrence());

        // Parcela 2: 33.33
        assertEquals(new BigDecimal("33.33"), savedBills.get(1).getAmount());
        assertEquals(2, savedBills.get(1).getInstallmentNumber());
        assertEquals("Fone Bluetooth (2/3)", savedBills.get(1).getTitle());
        assertEquals(startDate.plusMonths(1), savedBills.get(1).getDueDate());

        // Parcela 3: 33.33
        assertEquals(new BigDecimal("33.33"), savedBills.get(2).getAmount());
        assertEquals(3, savedBills.get(2).getInstallmentNumber());
        assertEquals("Fone Bluetooth (3/3)", savedBills.get(2).getTitle());
        assertEquals(startDate.plusMonths(2), savedBills.get(2).getDueDate());

        // Soma total rigorosamente exata: 33.34 + 33.33 + 33.33 = 100.00
        BigDecimal sum = savedBills.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    @DisplayName("Deve gerar transações e contas pagas para parcelas anteriores e pendentes a partir da atual")
    void create_Success_WithCurrentInstallment_PastInstallmentsGoToTransactionsAndBillsMarkedPaid() {
        when(installmentGroupRepository.save(any(InstallmentGroup.class))).thenAnswer(inv -> {
            InstallmentGroup g = inv.getArgument(0);
            ReflectionTestUtils.setField(g, "id", groupId);
            return g;
        });

        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Usuário comprou algo em 5x de R$ 100 e está na parcela 3
        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("500.00"),
                new BigDecimal("100.00"),
                5,
                3,
                "CARTAO",
                startDate,
                "Notebook Usado"
        );

        InstallmentGroup result = installmentService.create(sampleUser, dto);

        assertNotNull(result);
        assertEquals(5, result.getTotalInstallments());

        // 1. As 2 parcelas anteriores (1 e 2) devem ser salvas como Transações realizadas no passado
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTxs = txCaptor.getAllValues();

        assertEquals(2, savedTxs.size());

        // Parcela 1 paga 2 meses antes
        assertEquals(new BigDecimal("100.00"), savedTxs.get(0).getAmount());
        assertEquals(TransactionType.EXPENSE, savedTxs.get(0).getType());
        assertEquals("Notebook Usado (1/5)", savedTxs.get(0).getDescription());
        assertEquals(startDate.minusMonths(2), savedTxs.get(0).getDate());
        assertEquals(1, savedTxs.get(0).getInstallmentNumber());
        assertEquals(5, savedTxs.get(0).getTotalInstallments());

        // Parcela 2 paga 1 mês antes
        assertEquals(new BigDecimal("100.00"), savedTxs.get(1).getAmount());
        assertEquals(TransactionType.EXPENSE, savedTxs.get(1).getType());
        assertEquals("Notebook Usado (2/5)", savedTxs.get(1).getDescription());
        assertEquals(startDate.minusMonths(1), savedTxs.get(1).getDate());
        assertEquals(2, savedTxs.get(1).getInstallmentNumber());
        assertEquals(5, savedTxs.get(1).getTotalInstallments());

        // 2. No total são salvas 5 contas no banco (2 PAID e 3 PENDING)
        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(5)).save(captor.capture());
        List<Bill> savedBills = captor.getAllValues();

        assertEquals(5, savedBills.size());

        // Parcela 1: PAID, 2 meses antes
        assertEquals(BillStatus.PAID, savedBills.get(0).getStatus());
        assertEquals(startDate.minusMonths(2), savedBills.get(0).getDueDate());
        assertEquals(1, savedBills.get(0).getInstallmentNumber());

        // Parcela 2: PAID, 1 mês antes
        assertEquals(BillStatus.PAID, savedBills.get(1).getStatus());
        assertEquals(startDate.minusMonths(1), savedBills.get(1).getDueDate());
        assertEquals(2, savedBills.get(1).getInstallmentNumber());

        // Parcela 3: PENDING, startDate
        assertEquals(BillStatus.PENDING, savedBills.get(2).getStatus());
        assertEquals(startDate, savedBills.get(2).getDueDate());
        assertEquals(3, savedBills.get(2).getInstallmentNumber());
        assertEquals("Notebook Usado (3/5)", savedBills.get(2).getTitle());

        // Parcela 4: PENDING, startDate + 1 mês
        assertEquals(BillStatus.PENDING, savedBills.get(3).getStatus());
        assertEquals(startDate.plusMonths(1), savedBills.get(3).getDueDate());
        assertEquals(4, savedBills.get(3).getInstallmentNumber());

        // Parcela 5: PENDING, startDate + 2 meses
        assertEquals(BillStatus.PENDING, savedBills.get(4).getStatus());
        assertEquals(startDate.plusMonths(2), savedBills.get(4).getDueDate());
        assertEquals(5, savedBills.get(4).getInstallmentNumber());
    }

    @Test
    @DisplayName("Deve calcular valor total automaticamente quando apenas installmentAmount for informado")
    void create_Success_WithInstallmentAmountOnly_CalculatesTotal() {
        when(installmentGroupRepository.save(any(InstallmentGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        // Informa apenas o valor da parcela (R$ 80,00 em 4x)
        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                null,
                new BigDecimal("80.00"),
                4,
                1,
                "LAZER",
                startDate,
                "Ingressos Show"
        );

        InstallmentGroup result = installmentService.create(sampleUser, dto);

        assertNotNull(result);
        assertEquals(new BigDecimal("320.00"), result.getTotalAmount());

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(4)).save(captor.capture());
        List<Bill> savedBills = captor.getAllValues();

        for (Bill b : savedBills) {
            assertEquals(new BigDecimal("80.00"), b.getAmount());
        }
    }

    @Test
    @DisplayName("Deve lançar ConflictException se parcela atual for maior que o total de parcelas")
    void create_CurrentInstallmentGreaterThanTotal_ThrowsConflictException() {
        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("500.00"),
                null,
                5,
                6, // parcela 6 de 5 não existe
                "GERAL",
                startDate,
                "Erro Parcela"
        );

        assertThrows(ConflictException.class, () -> installmentService.create(sampleUser, dto));
        verifyNoInteractions(installmentGroupRepository);
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Deve distribuir centavos de forma balanceada em divisões não exatas (ex: R$ 100 em 7x)")
    void create_Success_OddDivisionBalancesAcrossInstallments() {
        when(installmentGroupRepository.save(any(InstallmentGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("100.00"),
                null,
                7,
                1,
                "VIAGEM",
                startDate,
                "Passagem"
        );

        installmentService.create(sampleUser, dto);

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(7)).save(captor.capture());
        List<Bill> bills = captor.getAllValues();

        // 100 / 7 = 14.28 com resto de 4 centavos -> primeiras 4 parcelas ganham 14.29, últimas 3 ganham 14.28
        for (int i = 0; i < 4; i++) {
            assertEquals(new BigDecimal("14.29"), bills.get(i).getAmount(), "Parcela " + (i + 1) + " deve ser 14.29");
        }
        for (int i = 4; i < 7; i++) {
            assertEquals(new BigDecimal("14.28"), bills.get(i).getAmount(), "Parcela " + (i + 1) + " deve ser 14.28");
        }

        BigDecimal sum = bills.stream().map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    @DisplayName("Deve lançar ConflictException se total de parcelas for menor que 2")
    void create_InstallmentsLessThanTwo_ThrowsConflictException() {
        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("500.00"),
                null,
                1,
                1,
                "GERAL",
                startDate,
                "Compra à vista"
        );

        assertThrows(ConflictException.class, () -> installmentService.create(sampleUser, dto));
        verifyNoInteractions(installmentGroupRepository);
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Deve lançar ConflictException se valor for nulo ou menor/igual a zero")
    void create_InvalidAmount_ThrowsConflictException() {
        InstallmentPurchaseCreateDTO dtoZero = new InstallmentPurchaseCreateDTO(
                BigDecimal.ZERO,
                null,
                3,
                1,
                "GERAL",
                startDate,
                "Teste"
        );

        assertThrows(ConflictException.class, () -> installmentService.create(sampleUser, dtoZero));
        verifyNoInteractions(installmentGroupRepository);
        verifyNoInteractions(billRepository);
    }

    @Test
    @DisplayName("Deve buscar por ID com Zero IDOR e lançar NotFoundException quando não pertencer ao usuário")
    void findById_ZeroIdorProtection() {
        InstallmentGroup group = new InstallmentGroup(sampleUser, "TV 4K", new BigDecimal("2400.00"), 12, "CASA", startDate);
        when(installmentGroupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));

        InstallmentGroup found = installmentService.findById(groupId, userId);
        assertNotNull(found);
        assertEquals("TV 4K", found.getDescription());

        UUID otherGroupId = UUID.randomUUID();
        when(installmentGroupRepository.findByIdAndUserId(otherGroupId, userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> installmentService.findById(otherGroupId, userId));
    }

    @Test
    @DisplayName("Deve excluir compra parcelada e suas contas vinculadas em definitivo")
    void delete_Success_DeletesGroup() {
        InstallmentGroup group = new InstallmentGroup(sampleUser, "Mesa", new BigDecimal("600.00"), 3, "MOVEIS", startDate);
        when(installmentGroupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));

        installmentService.delete(groupId, userId);

        verify(installmentGroupRepository).delete(group);
    }
}
