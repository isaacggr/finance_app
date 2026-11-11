package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.dto.ForgotPasswordRequestDTO;
import com.isaacggr.finance_app.dto.LoginRequestDTO;
import com.isaacggr.finance_app.dto.LoginResponseDTO;
import com.isaacggr.finance_app.dto.ResetPasswordRequestDTO;
import com.isaacggr.finance_app.dto.VerifyEmailRequestDTO;
import com.isaacggr.finance_app.exception.ForbiddenException;
import com.isaacggr.finance_app.exception.UnauthorizedException;
import com.isaacggr.finance_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final long expirationMillis;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            VerificationTokenService verificationTokenService,
            EmailService emailService,
            @Value("${jwt.expiration:86400000}") long expirationMillis
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.expirationMillis = expirationMillis;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        if (user.isDeleted() && !user.canReactivate(LocalDateTime.now())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        // 🔒 Bloqueio estrito de contas com e-mail pendente de ativação (HTTP 403 Forbidden)
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Por favor, confirme seu e-mail antes de efetuar login.");
        }

        boolean reactivated = false;
        if (user.isDeleted()) {
            user.reactivate();
            userRepository.save(user);
            reactivated = true;
        }

        String token = tokenService.generateToken(user);
        return reactivated
                ? LoginResponseDTO.reactivated(token, expirationMillis)
                : LoginResponseDTO.of(token, expirationMillis);
    }

    // ==========================================
    // CONFIRMAÇÃO E ATIVAÇÃO DE CONTA
    // ==========================================
    @Transactional
    public void verifyEmail(VerifyEmailRequestDTO dto) {
        VerificationToken verificationToken = verificationTokenService.validateTokenWithEmail(
                dto.email(),
                dto.code(),
                TokenType.EMAIL_VERIFICATION
        );
        User user = verificationToken.getUser();
        user.verifyEmail();
        userRepository.save(user);
    }

    // ==========================================
    // REENVIO DE E-MAIL DE CONFIRMAÇÃO
    // ==========================================
    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    if (!user.isEmailVerified()) {
                        VerificationToken token = verificationTokenService.createEmailVerificationToken(user);
                        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token.getToken());
                    }
                });
    }

    // ==========================================
    // RECUPERAÇÃO DE SENHA - SOLICITAÇÃO (OWASP Anti-Enumeração)
    // ==========================================
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.email())
                .filter(User::isActive)
                .ifPresent(user -> {
                    VerificationToken token = verificationTokenService.createPasswordResetToken(user);
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token.getToken());
                });
    }

    // ==========================================
    // RECUPERAÇÃO DE SENHA - REDEFINIÇÃO
    // ==========================================
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        VerificationToken verificationToken = verificationTokenService.validateTokenWithEmail(
                dto.email(),
                dto.code(),
                TokenType.PASSWORD_RESET
        );
        User user = verificationToken.getUser();
        user.updatePassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }
}

