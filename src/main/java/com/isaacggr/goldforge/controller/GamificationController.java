package com.isaacggr.goldforge.controller;

import com.isaacggr.goldforge.domain.entity.User;
import com.isaacggr.goldforge.dto.gamification.ClaimQuestResponseDTO;
import com.isaacggr.goldforge.dto.gamification.GameProfileResponseDTO;
import com.isaacggr.goldforge.dto.gamification.QuestResponseDTO;
import com.isaacggr.goldforge.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Gamificação & Quests", description = "Endpoints de progressão RPG, níveis, streaks de disciplina e missões financeiras (FinanGame)")
@SecurityRequirement(name = "bearerAuth")
public class GamificationController {

    private final GamificationService gamificationService;

    @Operation(summary = "Obter perfil de gamificação", description = "Retorna o nível RPG atual, total de XP, XP para o próximo nível, porcentagem da barra e streak de disciplina.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil recuperado com sucesso",
                    content = @Content(schema = @Schema(implementation = GameProfileResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/gamification/profile")
    public ResponseEntity<GameProfileResponseDTO> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gamificationService.getProfile(user));
    }

    @Operation(summary = "Realizar check-in diário", description = "Registra a presença e consciência financeira no dia, incrementando o streak de dias consecutivos e concedendo XP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check-in processado",
                    content = @Content(schema = @Schema(implementation = ClaimQuestResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PostMapping("/gamification/checkin")
    public ResponseEntity<ClaimQuestResponseDTO> checkin(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gamificationService.performDailyCheckin(user));
    }

    @Operation(summary = "Listar missões ativas e conquistas", description = "Retorna as missões diárias, semanais e marcos permanentes com o progresso atual do jogador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de missões retornada",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuestResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/quests")
    public ResponseEntity<List<QuestResponseDTO>> getQuests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gamificationService.getQuests(user));
    }

    @Operation(summary = "Resgatar recompensa de uma missão", description = "Resgata os pontos de experiência (XP) de uma missão concluída, creditando no perfil do jogador e podendo causar Level Up.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recompensa resgatada com sucesso",
                    content = @Content(schema = @Schema(implementation = ClaimQuestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Missão não concluída ou já resgatada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Missão não encontrada")
    })
    @PostMapping("/quests/{questId}/claim")
    public ResponseEntity<ClaimQuestResponseDTO> claimQuest(
            @AuthenticationPrincipal User user,
            @PathVariable UUID questId
    ) {
        return ResponseEntity.ok(gamificationService.claimQuest(user, questId));
    }
}
