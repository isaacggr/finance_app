package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.dto.LoginRequestDTO;
import com.isaacggr.finance_app.dto.LoginResponseDTO;
import com.isaacggr.finance_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints para login, ativação de conta e autenticação de usuários")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica o usuário com e-mail e senha e retorna o token JWT")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirmar ativação de e-mail", description = "Ativa a conta do usuário através do código numérico de 6 dígitos recebido por e-mail")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam @Parameter(description = "Código de 6 dígitos enviado por e-mail", example = "482910") String code
    ) {
        authService.verifyEmail(code);
        return ResponseEntity.ok(Map.of("message", "E-mail verificado com sucesso! Sua conta agora está ativa."));
    }

    @PostMapping("/resend-code")
    @Operation(summary = "Reenviar código de ativação", description = "Gera e dispara um novo código de 6 dígitos caso o anterior tenha expirado ou sido perdido")
    public ResponseEntity<Map<String, String>> resendCode(
            @RequestParam @Parameter(description = "E-mail da conta pendente de ativação", example = "usuario@example.com") String email
    ) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(Map.of("message", "Se o e-mail estiver cadastrado e pendente, um novo código foi enviado."));
    }
}

