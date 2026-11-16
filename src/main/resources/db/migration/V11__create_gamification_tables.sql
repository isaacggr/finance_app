-- ========================================================
-- V11: Criação das tabelas do motor de gamificação do FinanGame
-- Inclui perfil de jogo (XP acumulado, nível, streak de disciplina)
-- e missões ativas (quests diárias, semanais e conquistas permanentes)
-- com unicidade por período e exclusão em cascata (ON DELETE CASCADE).
-- ========================================================

CREATE TABLE IF NOT EXISTS user_game_profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    total_xp BIGINT NOT NULL DEFAULT 0,
    current_level INT NOT NULL DEFAULT 1,
    streak_days INT NOT NULL DEFAULT 0,
    last_checkin_date DATE DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_game_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_user_game_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Garante que todos os usuários existentes já possuam perfil de jogo inicializado
INSERT INTO user_game_profiles (
    id,
    user_id,
    total_xp,
    current_level,
    streak_days,
    last_checkin_date,
    created_at,
    updated_at
)
SELECT
    UUID_TO_BIN(UUID()),
    u.id,
    0,
    1,
    0,
    NULL,
    NOW(6),
    NOW(6)
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_game_profiles gp WHERE gp.user_id = u.id
);

CREATE TABLE IF NOT EXISTS user_quests (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    quest_code VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    xp_reward INT NOT NULL,
    current_progress INT NOT NULL DEFAULT 0,
    target_progress INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    period_key VARCHAR(30) NOT NULL,
    expires_at DATETIME(6) DEFAULT NULL,
    completed_at DATETIME(6) DEFAULT NULL,
    claimed_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user_quests_user_status (user_id, status),
    CONSTRAINT uk_user_quest_period UNIQUE (user_id, quest_code, period_key),
    CONSTRAINT fk_user_quests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
