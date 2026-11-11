package com.isaacggr.finance_app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserPasswordUpdateDTO;
import com.isaacggr.finance_app.dto.UserProfileUpdateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService verificationTokenService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
    }

    // ======================
    // CREATE
    // ======================
    public UserResponseDTO create(UserCreateDTO dto) {

        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            if (user.isActive()) {
                throw new ConflictException("Email já cadastrado");
            } else {
                throw new ConflictException(
                        "Existe uma conta desativada com este email. Faça login em até 30 dias para reativá-la ou aguarde a exclusão definitiva."
                );
            }
        });

        User user = new User(
                dto.name(),
                dto.email(),
                passwordEncoder.encode(dto.password())
        );

        User savedUser = userRepository.save(user);

        // Gera token temporário de ativação (24h) e dispara e-mail de confirmação
        VerificationToken token = verificationTokenService.createEmailVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), token.getToken());

        return UserResponseDTO.fromEntity(savedUser);
    }

    // ======================
    // DELETE (soft)
    // ======================
    public void delete(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado")
                );

        if (!user.isActive()) {
            throw new ConflictException("Usuário já foi deletado");
        }

        user.markAsDeleted();
        userRepository.save(user);
    }

    // ======================
    // EXPURGO DEFINITIVO APÓS 30 DIAS (LGPD)
    // Executa diariamente às 04:00 da manhã
    // ======================
    @Scheduled(cron = "0 0 4 * * ?")
    public int purgeExpiredDeletedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(User.ACCOUNT_RETENTION_DAYS);
        int purgedCount = userRepository.deleteDeletedUsersBefore(cutoff);
        if (purgedCount > 0) {
            log.info("🧹 [EXPURGO LGPD] {} conta(s) excluída(s) definitivamente após {} dias de retenção.",
                    purgedCount, User.ACCOUNT_RETENTION_DAYS);
        }
        return purgedCount;
    }

    // ======================
    // UPDATE PROFILE (Sem reenvio de e-mail)
    // ======================
    public UserResponseDTO updateProfile(UUID id, UserProfileUpdateDTO dto) {
        User user = findActiveUserById(id);

        String normalizedName = dto.name().trim();
        if (normalizedName.length() < 2 || normalizedName.length() > 50) {
            throw new BusinessException("O nome deve ter entre 2 e 50 caracteres");
        }

        user.changeName(normalizedName);
        return UserResponseDTO.fromEntity(userRepository.save(user));
    }

    // ======================
    // CHANGE PASSWORD (Autenticado com senha atual, sem código por e-mail)
    // ======================
    public void changePassword(UUID id, UserPasswordUpdateDTO dto) {
        User user = findActiveUserById(id);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta");
        }

        if (passwordEncoder.matches(dto.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual");
        }

        user.updatePassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    // ======================
    // FIND BY ID
    // ======================
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return UserResponseDTO.fromEntity(findActiveUserById(id));
    }

    private User findActiveUserById(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    // ======================
    // LIST ACTIVE USERS
    // ======================
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllActive() {

        return userRepository.findAllByDeletedFalse()
                .stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
    }
}
