package com.isaacggr.finance_app.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardSummaryDTO;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;
    private Logger emailServiceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        emailServiceLogger = (Logger) LoggerFactory.getLogger(EmailService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        emailServiceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        emailServiceLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    @DisplayName("maskEmail deve ocultar parte local mantendo formato diagnóstico mínimo e tratar nulos")
    void maskEmail_FormatsSafely() {
        assertEquals("is***@example.com", EmailService.maskEmail("isaac.secreto@example.com"));
        assertEquals("a***@b.com", EmailService.maskEmail("ab@b.com"));
        assertEquals("x***@b.com", EmailService.maskEmail("x@b.com"));
        assertEquals("***", EmailService.maskEmail(null));
        assertEquals("***", EmailService.maskEmail("   "));
        assertEquals("***", EmailService.maskEmail("invalido"));
    }

    @Test
    @DisplayName("Não deve expor código de verificação nem e-mail completo nos logs e deve escapar HTML no nome")
    void sendVerificationEmail_RedactsLogsAndEscapesHtml() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail(
                "isaac.secreto@example.com",
                "<script>alert('xss')</script>",
                "987654"
        );

        String allLogs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);

        assertFalse(allLogs.contains("987654"), "Código OTP nunca deve aparecer nos logs");
        assertFalse(allLogs.contains("isaac.secreto@example.com"), "E-mail completo não deve aparecer nos logs");
        assertTrue(allLogs.contains("is***@example.com"));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String html = extractHtmlContent(messageCaptor.getValue());
        assertFalse(html.contains("<script>alert('xss')</script>"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertTrue(html.contains("987654"));
    }

    @Test
    @DisplayName("Não deve expor código de reset nos logs e deve escapar HTML em reset de senha e relatório mensal")
    void sendPasswordResetAndMonthlyReport_RedactLogsAndEscapeHtml() throws Exception {
        MimeMessage resetMsg = new MimeMessage((Session) null);
        MimeMessage reportMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(resetMsg, reportMsg);

        emailService.sendPasswordResetEmail("maria.silva@example.com", "<b>Maria</b>", "654321");

        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("1000.00"),
                new BigDecimal("250.00"),
                new BigDecimal("750.00"),
                List.of(new CategoryExpenseDTO("<img src=x onerror=alert(1)>", new BigDecimal("250.00")))
        );
        emailService.sendMonthlyReportEmail("maria.silva@example.com", "<a href='http://evil'>Maria</a>", summary, "10/2026");

        String allLogs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);

        assertFalse(allLogs.contains("654321"));
        assertFalse(allLogs.contains("maria.silva@example.com"));
        assertTrue(allLogs.contains("ma***@example.com"));

        assertFalse(extractHtmlContent(resetMsg).contains("<b>Maria</b>"));
        assertTrue(extractHtmlContent(resetMsg).contains("&lt;b&gt;Maria&lt;/b&gt;"));

        String reportHtml = extractHtmlContent(reportMsg);
        assertFalse(reportHtml.contains("<a href='http://evil'>"));
        assertFalse(reportHtml.contains("<img src=x onerror=alert(1)>"));
        assertTrue(reportHtml.contains("&lt;img src=x onerror=alert(1)&gt;"));
    }

    private String extractHtmlContent(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof Multipart multipart) {
            return extractFromMultipart(multipart);
        }
        return "";
    }

    private String extractFromMultipart(Multipart multipart) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object partContent = part.getContent();
            if (partContent instanceof String s) {
                sb.append(s);
            } else if (partContent instanceof Multipart nested) {
                sb.append(extractFromMultipart(nested));
            }
        }
        return sb.toString();
    }
}
