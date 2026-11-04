package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.exception.ConflictException;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        verify(verificationTokenService, times(1)).createEmailVerificationToken(sampleUser);
        verify(emailService, times(1)).sendVerificationEmail(eq("isaac@example.com"), eq("Isaac Gregorio"), anyString());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o e-mail já estiver cadastrado e ativo")
    void create_ThrowsConflictException_WhenEmailAlreadyExists() {
        UserCreateDTO dto = new UserCreateDTO("Isaac Gregorio", "isaac@example.com", "password123");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> userService.create(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o e-mail pertencer a uma conta desativada")
    void create_ThrowsConflictException_WhenEmailBelongsToDeletedUser() {
        UserCreateDTO dto = new UserCreateDTO("Isaac Gregorio", "isaac@example.com", "password123");
        sampleUser.markAsDeleted();

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(sampleUser));

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
    @DisplayName("Deve marcar o usuário como deletado com sucesso")
    void delete_Success() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));

        userService.delete(id);

        assertTrue(sampleUser.isDeleted());
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
    @DisplayName("Deve retornar todos os usuários ativos")
    void findAllActive_Success() {
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(sampleUser));

        List<UserResponseDTO> result = userService.findAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleUser.getName(), result.get(0).name());
    }
}

