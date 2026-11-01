package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    // ✅ Buscar usuário por ID
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados públicos de um usuário cadastrado")
    public ResponseEntity<UserResponseDTO> findById(
            @PathVariable @Parameter(description = "Identificador único (UUID) do usuário", example = "e4c3b2a1-0000-4000-8000-123456789abc") UUID id
    ) {
        return ResponseEntity.ok(service.findById(id));
    }

    // ✅ Soft delete
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário", description = "Realiza a exclusão lógica (soft delete) do usuário pelo ID")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Identificador único (UUID) do usuário", example = "e4c3b2a1-0000-4000-8000-123456789abc") UUID id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
