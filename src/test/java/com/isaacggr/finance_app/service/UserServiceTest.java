package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.dto.user.UserCreateDTO;
import com.isaacggr.finance_app.dto.user.UserPasswordUpdateDTO;
import com.isaacggr.finance_app.dto.user.UserProfileUpdateDTO;
import com.isaacggr.finance_app.dto.user.UserResponseDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.UserNotificationPreferenceRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Isaac Gregorio", "isaac@example.com", "encodedPassword123");
    }

    @Test
    @DisplayName("Deve criar um usuário com sucesso quando o e-mail não existir")
    void create_Success() {
        UserCreateDTO dto = new UserCreateDTO("Isaac Gregorio", "isaac@example.com", "password123");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(verificationTokenService.createEmailVerificationToken(any(User.class)))
                .thenReturn(new VerificationToken(sampleUser, "token-123", TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24)));

        UserResponseDTO response = userService.create(dto);

        assertNotNull(response);
        assertEquals(dto.name(), response.name());
        assertEquals(dto.email(), response.email());
        verify(userRepository, times(1)).save(any(User.class));
        verify(preferenceRepository, times(1)).save(any());
        verify(verificationTokenService, times(1)).createEmailVerificationToken(sampleUser);
        verify(emailService, times(1)).sendVerificationEmail(eq("isaac@example.com"), eq("Isaac Gregorio"), anyString());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o e-mail já estiver cadastrado (conta ativa ou desativada)")
    void create_ThrowsConflictException_WhenEmailAlreadyRegistered() {
        UserCreateDTO dto = new UserCreateDTO("Isaac Gregorio", "isaac@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> userService.create(dto));

        sampleUser.markAsDeleted();
        assertThrows(ConflictException.class, () -> userService.create(dto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve buscar um usuário ativo por ID com sucesso")
    void findById_Success() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));

        UserResponseDTO response = userService.findById(id);

        assertNotNull(response);
        assertEquals(sampleUser.getName(), response.name());
        assertEquals(sampleUser.getEmail(), response.email());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o usuário não for encontrado pelo ID")
    void findById_ThrowsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.findById(id));
    }

    @Test
    @DisplayName("Deve marcar o usuário como deletado com carimbo temporal de retenção de 30 dias")
    void delete_Success() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));

        userService.delete(id);

        assertTrue(sampleUser.isDeleted());
        assertNotNull(sampleUser.getDeletedAt());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Deve expurgar definitivamente usuários desativados há mais de 30 dias")
    void purgeExpiredDeletedUsers_DeletesAccountsOlderThan30Days() {
        when(userRepository.deleteDeletedUsersBefore(any(LocalDateTime.class))).thenReturn(2);

        LocalDateTime beforeCall = LocalDateTime.now();
        int purged = userService.purgeExpiredDeletedUsers();
        LocalDateTime afterCall = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).deleteDeletedUsersBefore(cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertEquals(2, purged);
        assertFalse(cutoff.isBefore(beforeCall.minusDays(User.ACCOUNT_RETENTION_DAYS)));
        assertFalse(cutoff.isAfter(afterCall.minusDays(User.ACCOUNT_RETENTION_DAYS)));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar deletar um usuário já deletado")
    void delete_ThrowsConflictException_WhenAlreadyDeleted() {
        UUID id = UUID.randomUUID();
        sampleUser.markAsDeleted();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> userService.delete(id));
    }

    @Test
    @DisplayName("Deve atualizar o nome do usuário ativo removendo espaços extras")
    void updateProfile_Success() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        UserResponseDTO response = userService.updateProfile(id, new UserProfileUpdateDTO("  Isaac Atualizado  "));

        assertEquals("Isaac Atualizado", response.name());
        assertEquals("Isaac Atualizado", sampleUser.getName());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao atualizar nome com menos de 2 caracteres úteis")
    void updateProfile_ThrowsBusinessException_WhenTrimmedNameTooShort() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));

        assertThrows(BusinessException.class, () -> userService.updateProfile(id, new UserProfileUpdateDTO(" A ")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve alterar senha quando a senha atual confere e a nova é diferente")
    void changePassword_Success() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SenhaAtual123", "encodedPassword123")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha123", "encodedPassword123")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha123")).thenReturn("encodedNovaSenha");

        userService.changePassword(id, new UserPasswordUpdateDTO("SenhaAtual123", "NovaSenha123"));

        assertEquals("encodedNovaSenha", sampleUser.getPasswordHash());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando a senha atual estiver incorreta")
    void changePassword_ThrowsBusinessException_WhenCurrentPasswordMismatch() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SenhaErrada", "encodedPassword123")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.changePassword(id, new UserPasswordUpdateDTO("SenhaErrada", "NovaSenha123"))
        );
        assertEquals("Senha atual incorreta", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando a nova senha for igual à senha atual")
    void changePassword_ThrowsBusinessException_WhenNewPasswordEqualsCurrent() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SenhaAtual123", "encodedPassword123")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.changePassword(id, new UserPasswordUpdateDTO("SenhaAtual123", "SenhaAtual123"))
        );
        assertEquals("A nova senha deve ser diferente da senha atual", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar todos os usuários ativos")
    void findAllActive_Success() {
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(sampleUser));

        List<UserResponseDTO> result = userService.findAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleUser.getName(), result.get(0).name());
    }
}

