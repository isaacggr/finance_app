package com.isaacggr.finance_app;

import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration"
})
class FinaceAppApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @Test
    void contextLoads() {
    }

}
