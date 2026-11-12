package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.dto.dashboard.CategoryExpenseDTO;
import com.isaacggr.finance_app.dto.dashboard.DashboardSummaryDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
        log.info("📧 [ATIVACAO] E-mail de ativação preparado para {}", maskEmail(toEmail));

        String safeName = HtmlUtils.htmlEscape(name != null ? name : "");
        String safeCode = HtmlUtils.htmlEscape(code != null ? code : "");
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
                """.formatted(safeName, safeCode);

        sendHtmlEmail(toEmail, "Seu código de ativação - Finance App", htmlContent);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String code) {
        log.info("🔑 [RESET SENHA] E-mail de recuperação preparado para {}", maskEmail(toEmail));

        String safeName = HtmlUtils.htmlEscape(name != null ? name : "");
        String safeCode = HtmlUtils.htmlEscape(code != null ? code : "");
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
                """.formatted(safeName, safeCode);

        sendHtmlEmail(toEmail, "Código de Recuperação de Senha - Finance App", htmlContent);
    }

    @Async
    public void sendMonthlyReportEmail(String toEmail, String name, DashboardSummaryDTO summary, String periodName) {
        log.info("📊 [RELATORIO MENSAL] Enviando relatório ({}) para {}", periodName, maskEmail(toEmail));

        String incomeFormatted = formatCurrency(summary.totalIncome());
        String expenseFormatted = formatCurrency(summary.totalExpense());
        String balanceFormatted = formatCurrency(summary.balance());

        boolean isPositive = summary.balance() != null && summary.balance().compareTo(BigDecimal.ZERO) >= 0;
        String balanceColor = isPositive ? "#2e7d32" : "#c62828";
        String balanceBg = isPositive ? "#e8f5e9" : "#ffebee";

        StringBuilder categoryRows = new StringBuilder();
        if (summary.expensesByCategory() == null || summary.expensesByCategory().isEmpty()) {
            categoryRows.append("""
                    <tr>
                        <td colspan="2" style="padding: 14px; text-align: center; color: #757575; font-style: italic;">
                            Nenhuma despesa registrada neste período.
                        </td>
                    </tr>
                    """);
        } else {
            for (CategoryExpenseDTO cat : summary.expensesByCategory()) {
                String safeCategory = HtmlUtils.htmlEscape(cat.category() != null ? cat.category() : "");
                categoryRows.append("""
                        <tr style="border-bottom: 1px solid #f0f0f0;">
                            <td style="padding: 10px 14px; color: #424242; font-weight: 500;">%s</td>
                            <td style="padding: 10px 14px; text-align: right; font-weight: bold; color: #c62828;">%s</td>
                        </tr>
                        """.formatted(safeCategory, formatCurrency(cat.total())));
            }
        }

        String safePeriod = HtmlUtils.htmlEscape(periodName != null ? periodName : "");
        String safeName = HtmlUtils.htmlEscape(name != null ? name : "");
        String htmlContent = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 620px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                    <div style="text-align: center; border-bottom: 2px solid #f5f5f5; padding-bottom: 16px; margin-bottom: 20px;">
                        <h1 style="color: #1b5e20; margin: 0; font-size: 24px;">💰 Finance App</h1>
                        <p style="color: #616161; margin: 6px 0 0; font-size: 15px;">Resumo Financeiro Mensal • <strong>%s</strong></p>
                    </div>

                    <p style="font-size: 15px; color: #333333;">Olá, <strong>%s</strong>! Aqui está o fechamento consolidado das suas finanças:</p>

                    <!-- CARDS DE RESUMO -->
                    <div style="display: flex; margin: 20px 0; gap: 10px;">
                        <table style="width: 100%%; border-spacing: 8px;">
                            <tr>
                                <td style="background-color: #e8f5e9; padding: 14px; border-radius: 8px; text-align: center; width: 33%%;">
                                    <div style="font-size: 12px; color: #2e7d32; font-weight: bold; text-transform: uppercase;">Receitas</div>
                                    <div style="font-size: 18px; color: #1b5e20; font-weight: bold; margin-top: 4px;">%s</div>
                                </td>
                                <td style="background-color: #ffebee; padding: 14px; border-radius: 8px; text-align: center; width: 33%%;">
                                    <div style="font-size: 12px; color: #c62828; font-weight: bold; text-transform: uppercase;">Despesas</div>
                                    <div style="font-size: 18px; color: #b71c1c; font-weight: bold; margin-top: 4px;">%s</div>
                                </td>
                                <td style="background-color: %s; padding: 14px; border-radius: 8px; text-align: center; width: 33%%;">
                                    <div style="font-size: 12px; color: %s; font-weight: bold; text-transform: uppercase;">Saldo</div>
                                    <div style="font-size: 18px; color: %s; font-weight: bold; margin-top: 4px;">%s</div>
                                </td>
                            </tr>
                        </table>
                    </div>

                    <!-- TABELA DE GASTOS POR CATEGORIA -->
                    <div style="margin-top: 25px;">
                        <h3 style="color: #37474f; font-size: 16px; margin-bottom: 12px;">📊 Maiores Gastos por Categoria</h3>
                        <table style="width: 100%%; border-collapse: collapse; border: 1px solid #eeeeee; border-radius: 8px; overflow: hidden;">
                            <thead>
                                <tr style="background-color: #fafafa; border-bottom: 2px solid #eeeeee; text-align: left;">
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161;">Categoria</th>
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161; text-align: right;">Total Gasto</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>

                    <!-- FOOTER -->
                    <div style="margin-top: 35px; padding-top: 16px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #9e9e9e;">
                        <p style="margin: 0;">Relatório gerado automaticamente pelo seu <strong>Finance App</strong>.</p>
                        <p style="margin: 4px 0 0;">Continue no controle do seu dinheiro todos os dias! 🚀</p>
                    </div>
                </div>
                """.formatted(
                safePeriod,
                safeName,
                incomeFormatted,
                expenseFormatted,
                balanceBg,
                balanceColor,
                balanceColor,
                balanceFormatted,
                categoryRows.toString()
        );

        sendHtmlEmail(toEmail, "Relatório Financeiro Mensal (" + periodName + ") - Finance App", htmlContent);
    }

    @Async
    public void sendBillAlertEmail(String toEmail, String name, List<Bill> upcomingBills, int daysAhead) {
        log.info("🔔 [ALERTA CONTAS] Enviando alerta de {} conta(s) para {}", upcomingBills.size(), maskEmail(toEmail));

        BigDecimal totalUpcoming = upcomingBills.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder billRows = new StringBuilder();
        for (Bill bill : upcomingBills) {
            String safeTitle = HtmlUtils.htmlEscape(bill.getTitle());
            String safeCategory = HtmlUtils.htmlEscape(bill.getCategory());
            String statusLabel = HtmlUtils.htmlEscape(formatDueStatus(bill.getDueInDays()));

            billRows.append("""
                    <tr style="border-bottom: 1px solid #f0f0f0;">
                        <td style="padding: 10px 14px; color: #333333; font-weight: 500;">%s</td>
                        <td style="padding: 10px 14px; color: #616161;">%s</td>
                        <td style="padding: 10px 14px; text-align: center; color: #e65100; font-weight: 500;">%s (%s)</td>
                        <td style="padding: 10px 14px; text-align: right; font-weight: bold; color: #c62828;">%s</td>
                    </tr>
                    """.formatted(
                    safeTitle,
                    safeCategory,
                    bill.getDueDate().format(dateFormatter),
                    statusLabel,
                    formatCurrency(bill.getAmount())
            ));
        }

        String safeName = HtmlUtils.htmlEscape(name);
        String htmlContent = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 620px; margin: 0 auto; padding: 24px; border: 1px solid #ffe082; border-radius: 12px; background-color: #ffffff;">
                    <div style="text-align: center; border-bottom: 2px solid #fff8e1; padding-bottom: 16px; margin-bottom: 20px;">
                        <h1 style="color: #e65100; margin: 0; font-size: 24px;">⏰ Alerta de Contas a Pagar</h1>
                        <p style="color: #616161; margin: 6px 0 0; font-size: 15px;">Lembrete preventivo (janela de <strong>%d dias</strong>)</p>
                    </div>

                    <p style="font-size: 15px; color: #333333;">Olá, <strong>%s</strong>! Você possui conta(s) em aberto que merecem atenção:</p>

                    <!-- CARD DE TOTAL -->
                    <div style="background-color: #fff3e0; border: 1px solid #ffe0b2; padding: 16px; border-radius: 8px; text-align: center; margin: 20px 0;">
                        <div style="font-size: 12px; color: #e65100; font-weight: bold; text-transform: uppercase;">Total das Contas Alertadas</div>
                        <div style="font-size: 24px; color: #bf360c; font-weight: bold; margin-top: 4px;">%s</div>
                    </div>

                    <!-- TABELA DE CONTAS -->
                    <div style="margin-top: 25px;">
                        <h3 style="color: #37474f; font-size: 16px; margin-bottom: 12px;">📋 Detalhamento das Contas</h3>
                        <table style="width: 100%%; border-collapse: collapse; border: 1px solid #eeeeee; border-radius: 8px; overflow: hidden;">
                            <thead>
                                <tr style="background-color: #fafafa; border-bottom: 2px solid #eeeeee; text-align: left;">
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161;">Conta</th>
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161;">Categoria</th>
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161; text-align: center;">Vencimento</th>
                                    <th style="padding: 10px 14px; font-size: 13px; color: #616161; text-align: right;">Valor</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>

                    <!-- FOOTER -->
                    <div style="margin-top: 35px; padding-top: 16px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #9e9e9e;">
                        <p style="margin: 0;">Lembrete automático gerado pelo seu <strong>Finance App</strong>.</p>
                        <p style="margin: 4px 0 0;">Mantenha o seu planejamento em dia! 🚀</p>
                    </div>
                </div>
                """.formatted(
                daysAhead,
                safeName,
                formatCurrency(totalUpcoming),
                billRows.toString()
        );

        sendHtmlEmail(toEmail, "Alerta de Contas a Vencer - Finance App", htmlContent);
    }

    private String formatDueStatus(long dueInDays) {
        if (dueInDays == 0) {
            return "Vence hoje";
        }
        if (dueInDays < 0) {
            long overdueDays = Math.abs(dueInDays);
            return "Atrasada há " + overdueDays + (overdueDays == 1 ? " dia" : " dias");
        }
        return "Em " + dueInDays + (dueInDays == 1 ? " dia" : " dias");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "R$ 0,00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        return nf.format(amount);
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
            log.info("E-mail enviado com sucesso para {}", maskEmail(to));
        } catch (MessagingException | RuntimeException ex) {
            log.error("Falha ao enviar e-mail para {} ({}): {}", maskEmail(to), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
            return "***";
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex + 1);
        String visiblePrefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visiblePrefix + "***@" + domain;
    }
}

