package com.isaacggr.goldforge.service;

import com.isaacggr.goldforge.domain.entity.User;
import com.isaacggr.goldforge.domain.entity.UserNotificationPreference;
import com.isaacggr.goldforge.dto.notification.NotificationPreferenceResponseDTO;
import com.isaacggr.goldforge.dto.notification.NotificationPreferenceUpdateDTO;
import com.isaacggr.goldforge.exception.NotFoundException;
import com.isaacggr.goldforge.repository.UserNotificationPreferenceRepository;
import com.isaacggr.goldforge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class NotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(
            UserNotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponseDTO getPreferences(UUID userId) {
        findActiveUser(userId);
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponseDTO::fromEntity)
                .orElseGet(NotificationPreferenceResponseDTO::defaultValues);
    }

    @Transactional(readOnly = true)
    public UserNotificationPreference getEntityOrDefault(User user) {
        if (user == null || user.getId() == null) {
            return UserNotificationPreference.defaultFor(user);
        }
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> UserNotificationPreference.defaultFor(user));
    }

    public NotificationPreferenceResponseDTO updatePreferences(UUID userId, NotificationPreferenceUpdateDTO dto) {
        User user = findActiveUser(userId);

        UserNotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserNotificationPreference.defaultFor(user));

        preference.update(
                Boolean.TRUE.equals(dto.billAlertsEmailEnabled()),
                Boolean.TRUE.equals(dto.billAlertsInAppEnabled()),
                dto.billAlertDaysBefore(),
                Boolean.TRUE.equals(dto.monthlyReportEmailEnabled()),
                Boolean.TRUE.equals(dto.monthlyReportInAppEnabled()),
                Boolean.TRUE.equals(dto.budgetAlertsInAppEnabled()),
                Boolean.TRUE.equals(dto.goalAlertsInAppEnabled())
        );

        return NotificationPreferenceResponseDTO.fromEntity(preferenceRepository.save(preference));
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
