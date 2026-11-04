package com.isaacggr.finance_app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.UserRepository;

@Service
@Transactional
public class UserService {

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
                throw new ConflictException("Existe uma conta desativada com este email");
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
    }

    // ======================
    // FIND BY ID
    // ======================
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {

        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado")
                );

        return UserResponseDTO.fromEntity(user);
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
