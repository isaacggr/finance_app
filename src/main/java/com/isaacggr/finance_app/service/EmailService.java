package com.isaacggr.finance_app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Value("${spring.mail.username:noreply@financeapp.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String name, String code) {
        log.info("📧 [ATIVACAO] Código de verificação para {}: {}", toEmail, code);

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #2e7d32; text-align: center;">Bem-vindo ao Finance App, %s! 💰</h2>
                    <p>Falta apenas um passo para começar a gerenciar suas finanças com inteligência.</p>
                    <p>Seu código de ativação de conta é:</p>
                    <div style="text-align: center; margin: 25px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1b5e20; background-color: #e8f5e9; padding: 14px 28px; border-radius: 8px; border: 1px dashed #2e7d32;">
                            %s
                        </span>
                    </div>
                    <p style="font-size: 13px; color: #616161;">Copie o código acima e confirme no aplicativo ou Swagger.</p>
                    <p style="font-size: 12px; color: #9e9e9e; margin-top: 30px;">Este código expira em 24 horas e é de uso único. Se você não solicitou este cadastro, desconsidere esta mensagem.</p>
                </div>
                """.formatted(name, code);

        sendHtmlEmail(toEmail, "Seu código de ativação - Finance App", htmlContent);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String code) {
        log.info("🔑 [RESET SENHA] Código de recuperação para {}: {}", toEmail, code);

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #c62828; text-align: center;">Recuperação de Senha - Finance App 🔒</h2>
                    <p>Olá, %s. Recebemos uma solicitação para redefinir a sua senha.</p>
                    <p>Seu código de verificação para troca de senha é:</p>
                    <div style="text-align: center; margin: 25px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #b71c1c; background-color: #ffebee; padding: 14px 28px; border-radius: 8px; border: 1px dashed #c62828;">
                            %s
                        </span>
                    </div>
                    <p style="font-size: 13px; color: #616161;">Informe este código na tela de redefinição de senha.</p>
                    <p style="font-size: 12px; color: #9e9e9e; margin-top: 30px;">Este código expira em 30 minutos e é de uso único. Se você não solicitou a redefinição, proteja sua conta.</p>
                </div>
                """.formatted(name, code);

        sendHtmlEmail(toEmail, "Código de Recuperação de Senha - Finance App", htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@financeapp.com";
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para: {}", to);
        } catch (MessagingException | RuntimeException ex) {
            log.error("Falha ao enviar e-mail para {}: {}", to, ex.getMessage(), ex);
        }
    }
}

