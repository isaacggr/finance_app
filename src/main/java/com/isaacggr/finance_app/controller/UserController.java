package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Endpoints para cadastro e gerenciamento de perfil de usuários")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ✅ Criar usuário (Rota pública de cadastro)
    @PostMapping
    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário na aplicação com senha criptografada via BCrypt. Rota pública.")
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateDTO dto
    ) {
        UserResponseDTO response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ Obter perfil do usuário autenticado (Zero IDOR)
    @GetMapping("/me")
    @Operation(summary = "Obter perfil autenticado", description = "Retorna os dados cadastrais do próprio usuário autenticado. Essencial para o carregamento de sessão no front-end.")
    public ResponseEntity<UserResponseDTO> getProfile(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(user.getId()));
    }

    // ✅ Excluir própria conta (Zero IDOR)
    @DeleteMapping("/me")
    @Operation(summary = "Excluir própria conta", description = "Realiza a exclusão lógica (soft delete) da conta do próprio usuário autenticado")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user
    ) {
        service.delete(user.getId());
        return ResponseEntity.noContent().build();
    }
}
