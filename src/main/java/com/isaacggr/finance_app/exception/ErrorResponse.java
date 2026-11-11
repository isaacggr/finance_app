package com.isaacggr.finance_app.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Estrutura padronizada de resposta de erro da API")
public class ErrorResponse {

    @Schema(description = "Código HTTP do erro", example = "400")
    private int status;

    @Schema(description = "Mensagem resumida descrevendo o motivo do erro", example = "Erro de validação nos campos informados")
    private String message;

    @Schema(description = "Instante em que o erro ocorreu", example = "2026-11-11T16:45:00")
    private LocalDateTime timestamp;

    @Schema(description = "Mapa opcional de erros por campo (preenchido em falhas de Bean Validation)")
    private Map<String, String> errors;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String message, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
