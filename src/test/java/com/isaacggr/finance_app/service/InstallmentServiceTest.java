package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import com.isaacggr.finance_app.dto.installment.InstallmentPurchaseCreateDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.InstallmentGroupRepository;
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
                3,
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
    @DisplayName("Deve distribuir centavos de forma balanceada em divisões não exatas (ex: R$ 100 em 7x)")
    void create_Success_OddDivisionBalancesAcrossInstallments() {
        when(installmentGroupRepository.save(any(InstallmentGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        InstallmentPurchaseCreateDTO dto = new InstallmentPurchaseCreateDTO(
                new BigDecimal("100.00"),
                7,
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
                3,
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
