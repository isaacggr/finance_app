package com.isaacggr.finance_app.repository;

import com.isaacggr.finance_app.domain.entity.VerificationToken;
import com.isaacggr.finance_app.domain.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    // 🔍 Buscar token ativo por string e tipo (legado / fallback)
    Optional<VerificationToken> findByTokenAndTypeAndUsedFalse(String token, TokenType type);

    // 🔍 Buscar token ativo escopado pelo e-mail do usuário (Prevenção de colisão e Brute Force)
    Optional<VerificationToken> findByUserEmailAndTypeAndUsedFalse(String email, TokenType type);

    // 🔒 Buscar tokens pendentes de um determinado usuário para invalidar anteriores
    List<VerificationToken> findByUserIdAndTypeAndUsedFalse(UUID userId, TokenType type);

    // 🧹 Expurgo de tokens expirados (usado pela tarefa agendada @Scheduled)
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}

