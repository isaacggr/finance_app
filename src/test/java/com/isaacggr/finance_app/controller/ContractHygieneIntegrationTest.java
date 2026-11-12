package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.entity.GoalContribution;
import com.isaacggr.finance_app.domain.entity.Notification;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.BillRecurrence;
import com.isaacggr.finance_app.domain.enums.NotificationType;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.DailyTransactionAggregateDTO;
import com.isaacggr.finance_app.support.AbstractWebIntegrationTest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContractHygieneIntegrationTest extends AbstractWebIntegrationTest {

    private UUID userId;
    private User authenticatedUser;
    private String jwt;

    @BeforeEach
    void setUp() {
        authenticatedUser = createVerifiedUser("Isaac", "isaac@example.com", "SenhaForte@123");
        userId = authenticatedUser.getId();
        jwt = tokenService.generateToken(authenticatedUser);
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(authenticatedUser));
    }

    @Test
    @DisplayName("POST /transactions deve retornar 201 Created com header Location e valor normalizado")
    void createTransaction_Returns201WithLocationHeader() throws Exception {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", txId);
            return tx;
        });

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 89.9,
                                  "type": "EXPENSE",
                                  "category": "alimentacao",
                                  "date": "2026-11-11",
                                  "description": "Mercado"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/transactions/" + txId))
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.category").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.amount").value(89.9));
    }

    @Test
    @DisplayName("POST /transactions deve rejeitar valor com mais de 2 casas decimais ou acima do limite do banco")
    void createTransaction_Returns400_WhenAmountExceedsBounds() throws Exception {
        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 12.345,
                                  "type": "EXPENSE",
                                  "category": "ALIMENTACAO",
                                  "date": "2026-11-11"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.amount").exists());

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 10000000000000.00,
                                  "type": "EXPENSE",
                                  "category": "ALIMENTACAO",
                                  "date": "2026-11-11"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("GET /transactions deve aplicar paginação padrão (size=15) e desempate estável")
    @SuppressWarnings("unchecked")
    void listTransactions_UsesDefaultPageableWithStableSort() throws Exception {
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertEquals(0, used.getPageNumber());
        assertEquals(15, used.getPageSize());
        assertEquals(
                Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
                used.getSort()
        );
    }

    @Test
    @DisplayName("POST /budgets deve retornar 201 com Location e PUT /budgets/{id} deve atualizar com segurança")
    void createAndUpdateBudget_ContractWorks() throws Exception {
        UUID budgetId = UUID.randomUUID();
        Budget existing = new Budget(authenticatedUser, "ALIMENTACAO", new BigDecimal("800.00"), 11, 2026);
        ReflectionTestUtils.setField(existing, "id", budgetId);

        when(budgetRepository.existsByUserIdAndCategoryAndMonthAndYear(userId, "ALIMENTACAO", 11, 2026))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            if (b.getId() == null) {
                ReflectionTestUtils.setField(b, "id", budgetId);
            }
            return b;
        });

        mockMvc.perform(post("/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "alimentacao",
                                  "amount": 800.00,
                                  "month": 11,
                                  "year": 2026
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/budgets/" + budgetId))
                .andExpect(jsonPath("$.category").value("ALIMENTACAO"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId))
                .thenReturn(Optional.of(existing));

        mockMvc.perform(put("/budgets/" + budgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 1150.25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1150.25))
                .andExpect(jsonPath("$.category").value("ALIMENTACAO"));

        mockMvc.perform(put("/budgets/" + budgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "   ",
                                  "amount": 1150.25
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists());

        mockMvc.perform(delete("/budgets/" + budgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNoContent());

        verify(budgetRepository).delete(existing);
    }

    @Test
    @DisplayName("POST /auth/login deve omitir reactivated em login normal e reativar conta excluída dentro de 30 dias")
    void login_HandlesReactivationFlagCorrectly() throws Exception {
        when(userRepository.findByEmail("isaac@example.com")).thenReturn(Optional.of(authenticatedUser));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "isaac@example.com",
                                  "password": "SenhaForte@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.reactivated").doesNotExist());

        authenticatedUser.markAsDeleted();
        when(userRepository.save(authenticatedUser)).thenReturn(authenticatedUser);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "isaac@example.com",
                                  "password": "SenhaForte@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.reactivated").value(true));
    }

    @Test
    @DisplayName("POST /users deve retornar 201 Created com header Location apontando para /users/me")
    void createUser_Returns201WithUsersMeLocation() throws Exception {
        UUID newUserId = UUID.randomUUID();
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", newUserId);
            return u;
        });
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Usuario",
                                  "email": "novo@example.com",
                                  "password": "SenhaForte@123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/users/me"))
                .andExpect(jsonPath("$.email").value("novo@example.com"));
    }

    @Test
    @DisplayName("PATCH /users/me deve atualizar o nome do usuário autenticado sem envio de e-mail")
    void patchUsersMe_UpdatesNameSuccessfully() throws Exception {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.save(authenticatedUser)).thenReturn(authenticatedUser);

        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Isaac Editado  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Isaac Editado"))
                .andExpect(jsonPath("$.email").value("isaac@example.com"));

        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("PATCH /users/me/password deve exigir senha atual e alterar sem código por e-mail")
    void patchUsersMePassword_ValidatesCurrentPasswordAndChangesPassword() throws Exception {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.save(authenticatedUser)).thenReturn(authenticatedUser);

        mockMvc.perform(patch("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "SenhaErrada123!",
                                  "newPassword": "NovaSenhaForte@456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));

        mockMvc.perform(patch("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "SenhaForte@123",
                                  "newPassword": "NovaSenhaForte@456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha alterada com sucesso."));

        assertTrue(
                passwordEncoder.matches("NovaSenhaForte@456", authenticatedUser.getPasswordHash())
        );
    }

    @Test
    @DisplayName("GET /goals/primary, POST /goals, PATCH /goals/{id}/primary, aportes e DELETE /goals/{id} devem respeitar contrato e fallback")
    void goalsLifecycleAndPrimaryEndpoints_ContractWorks() throws Exception {
        when(goalRepository.findByUserIdAndPrimaryTrue(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/goals/primary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNoContent());

        UUID newGoalId = UUID.randomUUID();
        UUID oldGoalId = UUID.randomUUID();
        Goal olderGoal = new Goal(authenticatedUser, "Viagem Japão", null, new BigDecimal("10000.00"), null, true);
        ReflectionTestUtils.setField(olderGoal, "id", oldGoalId);

        when(goalRepository.findByUserIdAndPrimaryTrue(userId)).thenReturn(Optional.of(olderGoal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal g = inv.getArgument(0);
            if (g.getId() == null) {
                ReflectionTestUtils.setField(g, "id", newGoalId);
            }
            return g;
        });
        UUID initialContribId = UUID.randomUUID();
        when(goalContributionRepository.save(any(GoalContribution.class))).thenAnswer(inv -> {
            GoalContribution c = inv.getArgument(0);
            if (c.getId() == null) {
                ReflectionTestUtils.setField(c, "id", initialContribId);
            }
            return c;
        });

        mockMvc.perform(post("/goals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Reserva de Emergência",
                                  "targetAmount": 2000.00,
                                  "initialAmount": 500.00,
                                  "deadline": "2027-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/goals/" + newGoalId))
                .andExpect(jsonPath("$.primary").value(true))
                .andExpect(jsonPath("$.currentAmount").value(500.0))
                .andExpect(jsonPath("$.percentage").value(25.0))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        Goal newPrimary = new Goal(authenticatedUser, "Reserva de Emergência", null, new BigDecimal("2000.00"), LocalDate.of(2027, 12, 31), true);
        ReflectionTestUtils.setField(newPrimary, "id", newGoalId);
        newPrimary.applyContribution(new BigDecimal("500.00"));
        olderGoal.unmarkAsPrimary();

        when(goalRepository.findByUserIdAndPrimaryTrue(userId)).thenReturn(Optional.of(newPrimary));
        mockMvc.perform(get("/goals/primary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newGoalId.toString()))
                .andExpect(jsonPath("$.primary").value(true));

        when(goalRepository.findByIdAndUserId(oldGoalId, userId)).thenReturn(Optional.of(olderGoal));
        mockMvc.perform(patch("/goals/" + oldGoalId + "/primary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(oldGoalId.toString()))
                .andExpect(jsonPath("$.primary").value(true));

        when(goalRepository.findByIdAndUserId(newGoalId, userId)).thenReturn(Optional.of(newPrimary));
        mockMvc.perform(post("/goals/" + newGoalId + "/contributions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 1500.00,
                                  "note": "Bônus"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/goals/" + newGoalId + "/contributions/" + initialContribId))
                .andExpect(jsonPath("$.goal.currentAmount").value(2000.0))
                .andExpect(jsonPath("$.goal.percentage").value(100.0))
                .andExpect(jsonPath("$.goal.status").value("COMPLETED"));

        when(goalRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, oldGoalId))
                .thenReturn(Optional.of(newPrimary));
        mockMvc.perform(delete("/goals/" + oldGoalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNoContent());

        verify(goalRepository).delete(olderGoal);
        assertTrue(newPrimary.isPrimary());
    }

    @Test
    @DisplayName("Endpoints de /bills devem criar, listar próximos vencimentos, pagar gerando despesa/recorrência e excluir")
    void billsLifecycleUpcomingAndPayEndpoints_ContractWorks() throws Exception {
        UUID billId = UUID.randomUUID();
        UUID nextBillId = UUID.randomUUID();
        UUID generatedTxId = UUID.randomUUID();
        LocalDate today = LocalDate.now(Bill.DEFAULT_ZONE);

        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", billId);
            }
            return saved;
        });

        mockMvc.perform(post("/bills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Internet Fibra",
                                  "category": "moradia",
                                  "amount": 129.90,
                                  "dueDate": "%s",
                                  "recurrence": "MONTHLY"
                                }
                                """.formatted(today.plusDays(3))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/bills/" + billId))
                .andExpect(jsonPath("$.title").value("Internet Fibra"))
                .andExpect(jsonPath("$.category").value("MORADIA"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.dueInDays").value(3))
                .andExpect(jsonPath("$.recurrence").value("MONTHLY"));

        Bill overdueBill = new Bill(
                authenticatedUser,
                "Energia",
                null,
                "MORADIA",
                new BigDecimal("210.00"),
                today.minusDays(2),
                BillRecurrence.ONCE
        );
        ReflectionTestUtils.setField(overdueBill, "id", UUID.randomUUID());

        when(billRepository.findOpenBillsUpTo(userId, today.plusDays(7))).thenReturn(List.of(overdueBill));

        mockMvc.perform(get("/bills/upcoming")
                        .param("days", "7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$[0].dueInDays").value(-2));

        Bill monthlyBill = new Bill(
                authenticatedUser,
                "Internet Fibra",
                null,
                "MORADIA",
                new BigDecimal("129.90"),
                today.plusDays(3),
                BillRecurrence.MONTHLY
        );
        ReflectionTestUtils.setField(monthlyBill, "id", billId);

        when(billRepository.findByIdAndUserId(billId, userId)).thenReturn(Optional.of(monthlyBill));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", generatedTxId);
            return tx;
        });
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", nextBillId);
            }
            return saved;
        });

        mockMvc.perform(patch("/bills/" + billId + "/pay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bill.status").value("PAID"))
                .andExpect(jsonPath("$.bill.paidTransactionId").value(generatedTxId.toString()))
                .andExpect(jsonPath("$.generatedTransactionId").value(generatedTxId.toString()))
                .andExpect(jsonPath("$.nextBill.id").value(nextBillId.toString()))
                .andExpect(jsonPath("$.nextBill.dueDate").value(today.plusDays(3).plusMonths(1).toString()))
                .andExpect(jsonPath("$.nextBill.status").value("PENDING"));

        mockMvc.perform(put("/bills/" + billId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Internet Fibra",
                                  "category": "MORADIA",
                                  "amount": 139.90,
                                  "dueDate": "%s",
                                  "recurrence": "MONTHLY"
                                }
                                """.formatted(today.plusDays(5))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/bills/" + billId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNoContent());

        verify(billRepository).delete(monthlyBill);
    }

    @Test
    @DisplayName("GET /categories deve retornar sugestões acentuadas + customizadas do usuário e exigir autenticação")
    void categoriesEndpoint_ReturnsHybridCatalogAndProtectsRoute() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        when(transactionRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("ALIMENTAÇÃO", "IFOOD"));
        when(budgetRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("MORADIA"));
        when(billRepository.findDistinctCategoriesByUserId(userId))
                .thenReturn(List.of("ACADEMIA"));

        mockMvc.perform(get("/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='ALIMENTAÇÃO')].label").value("Alimentação"))
                .andExpect(jsonPath("$[?(@.name=='ALIMENTAÇÃO')].usedByUser").value(true))
                .andExpect(jsonPath("$[?(@.name=='ALIMENTAÇÃO')].defaultSuggested").value(true))
                .andExpect(jsonPath("$[?(@.name=='IFOOD')].defaultSuggested").value(false))
                .andExpect(jsonPath("$[?(@.name=='ACADEMIA')].usedByUser").value(true));
    }

    @Test
    @DisplayName("GET /dashboard/summary (previous/variation) e /dashboard/timeseries devem retornar séries e validações")
    void dashboardSummaryVariationAndTimeseriesEndpoints_WorkAsExpected() throws Exception {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 3);
        LocalDate prevStart = LocalDate.of(2026, 10, 29);
        LocalDate prevEnd = LocalDate.of(2026, 10, 31);

        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("1500.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.INCOME, prevStart, prevEnd))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(userId, TransactionType.EXPENSE, prevStart, prevEnd))
                .thenReturn(new BigDecimal("400.00"));
        when(transactionRepository.sumAmountByCategory(userId, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of());

        mockMvc.perform(get("/dashboard/summary")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-03")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.variation.incomePercentage").value(50.0))
                .andExpect(jsonPath("$.variation.balanceDifference").value(400.0));

        when(transactionRepository.aggregateByDateAndType(userId, start, end))
                .thenReturn(List.of(
                        new DailyTransactionAggregateDTO(LocalDate.of(2026, 11, 1), TransactionType.INCOME, new BigDecimal("1500.00")),
                        new DailyTransactionAggregateDTO(LocalDate.of(2026, 11, 3), TransactionType.EXPENSE, new BigDecimal("500.00"))
                ));

        mockMvc.perform(get("/dashboard/timeseries")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-03")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("DAILY"))
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.points[1].period").value("2026-11-02"))
                .andExpect(jsonPath("$.points[1].cumulativeBalance").value(1500.0))
                .andExpect(jsonPath("$.points[2].cumulativeBalance").value(1000.0));

        mockMvc.perform(get("/dashboard/timeseries")
                        .param("startDate", "2026-11-10")
                        .param("endDate", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Endpoints de /users/me/notification-preferences e /notifications devem operar com Zero IDOR")
    void notificationPreferencesAndNotificationsEndpoints_LifecycleWorks() throws Exception {
        when(userNotificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        mockMvc.perform(get("/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billAlertsEmailEnabled").value(true))
                .andExpect(jsonPath("$.billAlertDaysBefore").value(3));

        when(userNotificationPreferenceRepository.save(any(UserNotificationPreference.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(put("/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billAlertsEmailEnabled": false,
                                  "billAlertsInAppEnabled": true,
                                  "billAlertDaysBefore": 5,
                                  "monthlyReportEmailEnabled": false,
                                  "monthlyReportInAppEnabled": true,
                                  "budgetAlertsInAppEnabled": true,
                                  "goalAlertsInAppEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billAlertsEmailEnabled").value(false))
                .andExpect(jsonPath("$.billAlertDaysBefore").value(5))
                .andExpect(jsonPath("$.goalAlertsInAppEnabled").value(false));

        mockMvc.perform(put("/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billAlertsEmailEnabled": true,
                                  "billAlertsInAppEnabled": true,
                                  "billAlertDaysBefore": 0,
                                  "monthlyReportEmailEnabled": true,
                                  "monthlyReportInAppEnabled": true,
                                  "budgetAlertsInAppEnabled": true,
                                  "goalAlertsInAppEnabled": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.billAlertDaysBefore").exists());

        UUID notifId = UUID.randomUUID();
        Notification notif = new Notification(
                authenticatedUser, NotificationType.BILL_DUE_TODAY,
                "Conta vence hoje", "Sua conta vence hoje", "BILL", UUID.randomUUID(), "KEY_1"
        );
        ReflectionTestUtils.setField(notif, "id", notifId);

        when(notificationRepository.findByUserIdAndReadFalse(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notif)));
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(1L);
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(notif)).thenReturn(notif);
        when(notificationRepository.markAllAsReadByUserId(eq(userId), any())).thenReturn(1);

        mockMvc.perform(get("/notifications")
                        .param("unreadOnly", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notifId.toString()))
                .andExpect(jsonPath("$.content[0].read").value(false));

        mockMvc.perform(get("/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/notifications/" + notifId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(patch("/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));

        UUID foreignId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(foreignId, userId)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/notifications/" + foreignId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/notifications/" + notifId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isNoContent());
        verify(notificationRepository).delete(notif);
    }

    @Test
    @DisplayName("OpenAPI /v3/api-docs deve achatar Pageable, expor PATCH /users/me e documentar respostas de erro")
    void openApiSpec_FlattensPageableAndDocumentsErrorsAndProfileEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
                .andExpect(jsonPath("$.paths['/budgets/{id}'].put.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/budgets/{id}'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/budgets/{id}'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/budgets/{id}'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/users/me'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/users/me'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/users/me/password'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/users/me/password'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/users/me/notification-preferences'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/users/me/notification-preferences'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/categories'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/dashboard/timeseries'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/notifications'].get.parameters[?(@.name=='page')]").exists())
                .andExpect(jsonPath("$.paths['/notifications'].get.parameters[?(@.name=='pageable')]").isEmpty())
                .andExpect(jsonPath("$.paths['/notifications/unread-count'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/notifications/{notificationId}/read'].patch.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/notifications/read-all'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/goals'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/goals/primary'].get.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/goals/{goalId}/primary'].patch.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/goals/{goalId}/contributions'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/bills'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/bills/upcoming'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/bills/{billId}'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/bills/{billId}/pay'].patch.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/bills/{billId}/pay'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/transactions'].get.parameters[?(@.name=='page')]").exists())
                .andExpect(jsonPath("$.paths['/transactions'].get.parameters[?(@.name=='pageable')]").isEmpty());
    }
}
