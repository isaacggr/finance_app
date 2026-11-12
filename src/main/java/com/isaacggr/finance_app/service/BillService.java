package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BillService {

    public static final int DEFAULT_UPCOMING_DAYS = 7;
    public static final int MAX_UPCOMING_DAYS = 90;

    private final BillRepository billRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    public BillService(
            BillRepository billRepository,
            TransactionService transactionService,
            NotificationService notificationService,
            NotificationPreferenceService preferenceService
    ) {
        this.billRepository = billRepository;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    public BillResponseDTO create(User user, BillCreateDTO dto) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        Bill bill = new Bill(
                user,
                dto.title(),
                dto.description(),
                dto.category(),
                dto.amount(),
                dto.dueDate(),
                dto.recurrence()
        );

        Bill saved = billRepository.save(bill);
        notifyIfDueWithinWindow(user, saved);
        return BillResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BillResponseDTO> listAll(UUID userId, BillStatus statusFilter) {
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);
        List<Bill> bills;

        if (statusFilter == null) {
            bills = billRepository.findByUserIdOrdered(userId);
        } else {
            bills = switch (statusFilter) {
                case PAID -> billRepository.findByUserIdAndStatusOrderByPaidAtDescDueDateDesc(userId, BillStatus.PAID);
                case OVERDUE -> billRepository.findOverdueByUserId(userId, today);
                case PENDING -> billRepository.findUpcomingPendingByUserId(userId, today);
            };
        }

        return bills.stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponseDTO> listUpcoming(UUID userId, Integer days) {
        int effectiveDays = normalizeUpcomingDays(days);
        LocalDate endDate = LocalDate.now(Bill.DEFAULT_ZONE).plusDays(effectiveDays);

        return billRepository.findOpenBillsUpTo(userId, endDate)
                .stream()
                .map(BillResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillResponseDTO findById(UUID billId, UUID userId) {
        return BillResponseDTO.fromEntity(findOwnedBill(billId, userId));
    }

    public BillResponseDTO update(UUID billId, UUID userId, BillUpdateDTO dto) {
        Bill bill = findOwnedBill(billId, userId);
        if (bill.isPaid()) {
            throw new ConflictException("Não é possível editar uma conta já paga");
        }

        bill.update(
                dto.title(),
                dto.description(),
                dto.category(),
                dto.amount(),
                dto.dueDate(),
                dto.recurrence()
        );

        Bill saved = billRepository.save(bill);
        notifyIfDueWithinWindow(saved.getUser(), saved);
        return BillResponseDTO.fromEntity(saved);
    }

    /**
     * Marca a conta como paga, gera opcionalmente a despesa correspondente em /transactions (padrão true)
     * e, para contas recorrentes (MONTHLY/YEARLY), agenda automaticamente o próximo vencimento.
     */
    public BillPayResponseDTO pay(UUID billId, User user, BillPayRequestDTO dto) {
        if (user == null || !user.isActive()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        Bill bill = findOwnedBill(billId, user.getId());
        if (bill.isPaid()) {
            throw new ConflictException("Esta conta já foi marcada como paga");
        }

        LocalDate effectivePaidDate = (dto != null && dto.paidDate() != null)
                ? dto.paidDate()
                : LocalDate.now(Bill.DEFAULT_ZONE);

        boolean shouldCreateTransaction = (dto == null || dto.createTransaction() == null)
                || dto.createTransaction();

        UUID generatedTransactionId = null;
        if (shouldCreateTransaction) {
            Transaction generatedTx = transactionService.create(
                    user,
                    bill.getAmount(),
                    TransactionType.EXPENSE,
                    bill.getCategory(),
                    effectivePaidDate,
                    buildGeneratedTransactionDescription(bill)
            );
            generatedTransactionId = generatedTx.getId();
        }

        bill.markAsPaid(effectivePaidDate, generatedTransactionId);
        Bill savedBill = billRepository.save(bill);

        BillResponseDTO nextBillDto = bill.createNextRecurringBill()
                .map(billRepository::save)
                .map(BillResponseDTO::fromEntity)
                .orElse(null);

        return new BillPayResponseDTO(
                BillResponseDTO.fromEntity(savedBill),
                generatedTransactionId,
                nextBillDto
        );
    }

    public void delete(UUID billId, UUID userId) {
        Bill bill = findOwnedBill(billId, userId);
        billRepository.delete(bill);
    }

    private Bill findOwnedBill(UUID billId, UUID userId) {
        return billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new NotFoundException("Conta a pagar não encontrada"));
    }

    private void notifyIfDueWithinWindow(User user, Bill bill) {
        if (bill.isPaid() || user == null) {
            return;
        }
        int alertDays = preferenceService.getEntityOrDefault(user).getBillAlertDaysBefore();
        long dueInDays = bill.getDueInDaysOn(LocalDate.now(Bill.DEFAULT_ZONE));
        if (dueInDays <= alertDays) {
            notificationService.notifyBillDueAlert(user, bill, dueInDays);
        }
    }

    private static int normalizeUpcomingDays(Integer days) {
        if (days == null) {
            return DEFAULT_UPCOMING_DAYS;
        }
        if (days < 1 || days > MAX_UPCOMING_DAYS) {
            throw new IllegalArgumentException("A janela de dias deve estar entre 1 e " + MAX_UPCOMING_DAYS);
        }
        return days;
    }

    private static String buildGeneratedTransactionDescription(Bill bill) {
        String base = "Pagamento: " + bill.getTitle();
        return base.length() <= 255 ? base : base.substring(0, 255);
    }
}
