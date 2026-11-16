package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.dto.installment.InstallmentPurchaseCreateDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.InstallmentGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InstallmentService {

    private final InstallmentGroupRepository installmentGroupRepository;
    private final BillRepository billRepository;

    public InstallmentService(
            InstallmentGroupRepository installmentGroupRepository,
            BillRepository billRepository
    ) {
        this.installmentGroupRepository = installmentGroupRepository;
        this.billRepository = billRepository;
    }

    /**
     * Cria uma compra parcelada gerando automaticamente N contas a pagar (bills) sob o mesmo grupo.
     * Utiliza o padrão bancário de distribuição de resíduo (Remainder Distribution) para garantir
     * que a soma das parcelas seja rigorosamente igual ao valor total, sem perda de centavos.
     */
    public InstallmentGroup create(User user, InstallmentPurchaseCreateDTO dto) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        if (dto.totalAmount() == null || dto.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("O valor total da compra deve ser maior que zero");
        }

        if (dto.totalInstallments() == null || dto.totalInstallments() < 2) {
            throw new ConflictException("O número total de parcelas deve ser de no mínimo 2");
        }

        if (dto.startDate() == null) {
            throw new ConflictException("A data da primeira parcela é obrigatória");
        }

        BigDecimal normalizedTotal = Transaction.normalizeMonetaryAmount(dto.totalAmount());
        String normalizedCategory = Transaction.normalizeCategory(dto.category());

        InstallmentGroup group = new InstallmentGroup(
                user,
                dto.description(),
                normalizedTotal,
                dto.totalInstallments(),
                normalizedCategory,
                dto.startDate()
        );

        InstallmentGroup savedGroup = installmentGroupRepository.save(group);

        // Algoritmo de distribuição exata de centavos (Remainder Distribution)
        int totalInstallments = dto.totalInstallments();
        BigDecimal baseInstallmentAmount = normalizedTotal.divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.FLOOR
        );

        BigDecimal remainder = normalizedTotal.subtract(
                baseInstallmentAmount.multiply(BigDecimal.valueOf(totalInstallments))
        );
        int remainderCents = remainder.movePointRight(2).intValueExact();

        List<Bill> generatedBills = new ArrayList<>();

        for (int i = 1; i <= totalInstallments; i++) {
            BigDecimal installmentAmount = (i <= remainderCents)
                    ? baseInstallmentAmount.add(new BigDecimal("0.01"))
                    : baseInstallmentAmount;

            LocalDate installmentDate = dto.startDate().plusMonths(i - 1);
            String installmentTitle = String.format("%s (%d/%d)", dto.description().trim(), i, totalInstallments);
            String installmentDesc = String.format("Parcela %d de %d da compra parcelada", i, totalInstallments);

            Bill installmentBill = new Bill(
                    user,
                    installmentTitle,
                    installmentDesc,
                    normalizedCategory,
                    installmentAmount,
                    installmentDate,
                    BillRecurrence.ONCE,
                    savedGroup,
                    i,
                    totalInstallments
            );

            Bill savedBill = billRepository.save(installmentBill);
            generatedBills.add(savedBill);
        }

        savedGroup.getBills().addAll(generatedBills);
        return savedGroup;
    }

    @Transactional(readOnly = true)
    public InstallmentGroup findById(UUID installmentGroupId, UUID userId) {
        return installmentGroupRepository
                .findByIdAndUserId(installmentGroupId, userId)
                .orElseThrow(() -> new NotFoundException("Compra parcelada não encontrada"));
    }

    @Transactional(readOnly = true)
    public Page<InstallmentGroup> listByUser(UUID userId, Pageable pageable) {
        return installmentGroupRepository.findByUserId(userId, pageable);
    }

    /**
     * Exclui em definitivo a compra parcelada e todas as suas contas vinculadas (Hard Delete em cascata).
     */
    public void delete(UUID installmentGroupId, UUID userId) {
        InstallmentGroup group = findById(installmentGroupId, userId);
        installmentGroupRepository.delete(group);
    }
}
