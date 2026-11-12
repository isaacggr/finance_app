package com.isaacggr.finance_app.support;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.BudgetRepository;
import com.isaacggr.finance_app.repository.GoalContributionRepository;
import com.isaacggr.finance_app.repository.GoalRepository;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import com.isaacggr.finance_app.repository.VerificationTokenRepository;
import com.isaacggr.finance_app.service.TokenService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

/**
 * Base compartilhada para testes de integração HTTP sem banco real.
 * Garante configuração idêntica para reaproveitamento único do TestContext do Spring.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration"
})
public abstract class AbstractWebIntegrationTest {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    protected Filter springSecurityFilterChain;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected TransactionRepository transactionRepository;

    @MockitoBean
    protected BudgetRepository budgetRepository;

    @MockitoBean
    protected GoalRepository goalRepository;

    @MockitoBean
    protected GoalContributionRepository goalContributionRepository;

    @MockitoBean
    protected BillRepository billRepository;

    @MockitoBean
    protected VerificationTokenRepository verificationTokenRepository;

    @MockitoBean
    protected JavaMailSender mailSender;

    protected MockMvc mockMvc;

    @BeforeEach
    void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    protected User createVerifiedUser(String name, String email, String rawPassword) {
        User user = new User(name, email, passwordEncoder.encode(rawPassword));
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
