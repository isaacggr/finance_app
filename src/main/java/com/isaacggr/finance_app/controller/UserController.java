package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.dto.UserCreateDTO;
import com.isaacggr.finance_app.dto.UserResponseDTO;
import com.isaacggr.finance_app.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ✅ Criar usuário
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateDTO dto
    ) {
        UserResponseDTO response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.findById(id));
    }

    // ✅ Soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
