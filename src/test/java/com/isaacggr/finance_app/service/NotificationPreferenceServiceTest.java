package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.dto.notification.NotificationPreferenceResponseDTO;
import com.isaacggr.finance_app.dto.notification.NotificationPreferenceUpdateDTO;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.UserNotificationPreferenceRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("Isaac Gregorio", "isaac@example.com", "hash123");
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    @DisplayName("Deve retornar preferências padrão sem gravar quando usuário ainda não tiver registro salvo")
    void getPreferences_ReturnsDefaultsWhenAbsent() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotificationPreferenceResponseDTO dto = service.getPreferences(userId);

        assertTrue(dto.billAlertsEmailEnabled());
        assertTrue(dto.billAlertsInAppEnabled());
        assertEquals(3, dto.billAlertDaysBefore());
        assertTrue(dto.monthlyReportEmailEnabled());
        assertTrue(dto.monthlyReportInAppEnabled());
        assertTrue(dto.budgetAlertsInAppEnabled());
        assertTrue(dto.goalAlertsInAppEnabled());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar preferências criando registro caso ainda não exista")
    void updatePreferences_CreatesOrUpdatesSuccessfully() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserNotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferenceUpdateDTO dto = new NotificationPreferenceUpdateDTO(
                false, true, 5, false, true, true, false
        );

        NotificationPreferenceResponseDTO updated = service.updatePreferences(userId, dto);

        assertFalse(updated.billAlertsEmailEnabled());
        assertTrue(updated.billAlertsInAppEnabled());
        assertEquals(5, updated.billAlertDaysBefore());
        assertFalse(updated.monthlyReportEmailEnabled());
        assertTrue(updated.monthlyReportInAppEnabled());
        assertTrue(updated.budgetAlertsInAppEnabled());
        assertFalse(updated.goalAlertsInAppEnabled());
        verify(preferenceRepository).save(any(UserNotificationPreference.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao consultar preferências de usuário inexistente ou desativado")
    void getPreferences_ThrowsNotFoundWhenUserMissing() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPreferences(userId));
    }
}
