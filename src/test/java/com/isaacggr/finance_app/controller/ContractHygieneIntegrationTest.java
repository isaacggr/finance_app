package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import com.isaacggr.finance_app.repository.VerificationTokenRepository;
import com.isaacggr.finance_app.service.TokenService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration"
})
class ContractHygieneIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private BudgetRepository budgetRepository;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;
    private UUID userId;
    private User authenticatedUser;
    private String jwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        userId = UUID.randomUUID();
        authenticatedUser = new User("Isaac", "isaac@example.com", passwordEncoder.encode("SenhaForte@123"));
        authenticatedUser.verifyEmail();
        ReflectionTestUtils.setField(authenticatedUser, "id", userId);

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
    @DisplayName("OpenAPI /v3/api-docs deve achatar Pageable (@ParameterObject) e expor PUT /budgets/{id}")
    void openApiSpec_FlattensPageableAndExposesPutBudget() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/budgets/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/transactions'].get.parameters[?(@.name=='page')]").exists())
                .andExpect(jsonPath("$.paths['/transactions'].get.parameters[?(@.name=='pageable')]").isEmpty());
    }
}
