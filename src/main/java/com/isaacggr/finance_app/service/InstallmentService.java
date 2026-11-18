package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.InstallmentGroup;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.installment.InstallmentPurchaseCreateDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.InstallmentGroupRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
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
    private final TransactionRepository transactionRepository;

    public InstallmentService(
            InstallmentGroupRepository installmentGroupRepository,
            BillRepository billRepository,
            TransactionRepository transactionRepository
    ) {
        this.installmentGroupRepository = installmentGroupRepository;
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Cria uma compra parcelada gerando automaticamente N contas a pagar (bills) sob o mesmo grupo.
     * Permite que o usuário informe em qual parcela a compra está atualmente, gerando apenas
     * as obrigações pendentes futuras.
     * Utiliza o padrão bancário de distribuição de resíduo (Remainder Distribution) para garantir
     * que a soma das parcelas seja rigorosamente igual ao valor total, sem perda de centavos.
     */
    public InstallmentGroup create(User user, InstallmentPurchaseCreateDTO dto) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        if (dto.totalInstallments() == null || dto.totalInstallments() < 2) {
            throw new ConflictException("O número total de parcelas deve ser de no mínimo 2");
        }

        int totalInstallments = dto.totalInstallments();
        int currentInstallment = (dto.currentInstallment() != null && dto.currentInstallment() >= 1)
                ? dto.currentInstallment()
                : 1;

        if (currentInstallment > totalInstallments) {
            throw new ConflictException("A parcela atual não pode ser maior que o número total de parcelas");
        }

        BigDecimal rawTotal = dto.totalAmount();
        if (rawTotal == null || rawTotal.compareTo(BigDecimal.ZERO) <= 0) {
            if (dto.installmentAmount() != null && dto.installmentAmount().compareTo(BigDecimal.ZERO) > 0) {
                rawTotal = dto.installmentAmount().multiply(BigDecimal.valueOf(totalInstallments));
            } else {
                throw new ConflictException("O valor total da compra ou o valor da parcela deve ser maior que zero");
            }
        }

        if (dto.startDate() == null) {
            throw new ConflictException("A data da primeira parcela é obrigatória");
        }

        BigDecimal normalizedTotal = Transaction.normalizeMonetaryAmount(rawTotal);
        String normalizedCategory = Transaction.normalizeCategory(dto.category());

        InstallmentGroup group = new InstallmentGroup(
                user,
                dto.description(),
                normalizedTotal,
                totalInstallments,
                normalizedCategory,
                dto.startDate()
        );

        InstallmentGroup savedGroup = installmentGroupRepository.save(group);

        // Algoritmo de distribuição exata de centavos (Remainder Distribution)
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
        List<Transaction> pastTransactions = new ArrayList<>();

        // 1. Gera e liquida no extrato de transações as parcelas anteriores à atual (já pagas no passado)
        for (int i = 1; i < currentInstallment; i++) {
            BigDecimal installmentAmount = (i <= remainderCents)
                    ? baseInstallmentAmount.add(new BigDecimal("0.01"))
                    : baseInstallmentAmount;

            // Recua mês a mês com base na data da parcela atual
            LocalDate pastDate = dto.startDate().minusMonths(currentInstallment - i);
            String title = String.format("%s (%d/%d)", dto.description().trim(), i, totalInstallments);
            String desc = String.format("Parcela %d de %d da compra parcelada (paga em meses anteriores)", i, totalInstallments);

            // Registra a transação de despesa no mês correspondente
            Transaction tx = new Transaction(
                    user,
                    installmentAmount,
                    TransactionType.EXPENSE,
                    normalizedCategory,
                    pastDate,
                    title,
                    savedGroup,
                    i,
                    totalInstallments
            );
            Transaction savedTx = transactionRepository.save(tx);
            pastTransactions.add(savedTx);

            // Cria o registro da conta como PAGA vinculada à transação gerada
            Bill pastBill = new Bill(
                    user,
                    title,
                    desc,
                    normalizedCategory,
                    installmentAmount,
                    pastDate,
                    BillRecurrence.ONCE,
                    savedGroup,
                    i,
                    totalInstallments
            );
            pastBill.markAsPaid(pastDate, savedTx.getId());
            Bill savedPastBill = billRepository.save(pastBill);
            generatedBills.add(savedPastBill);
        }

        // 2. Gera contas a pagar pendentes para a parcela atual e futuras (ex: da parcela 3 até 10)
        for (int i = currentInstallment; i <= totalInstallments; i++) {
            BigDecimal installmentAmount = (i <= remainderCents)
                    ? baseInstallmentAmount.add(new BigDecimal("0.01"))
                    : baseInstallmentAmount;

            // A data avança mês a mês a partir da data de vencimento da parcela atual
            LocalDate installmentDate = dto.startDate().plusMonths(i - currentInstallment);
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
        savedGroup.getTransactions().addAll(pastTransactions);
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
