package com.isaacggr.finance_app.controller;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.dto.category.CategoryOptionDTO;
import com.isaacggr.finance_app.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Catálogo híbrido de categorias (sugestões padrão acentuadas + categorias customizadas já usadas)")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(
            summary = "Listar sugestões e categorias do usuário",
            description = "Retorna sugestões padrão acentuadas combinadas com todas as categorias já utilizadas pelo usuário em transações, orçamentos e contas. Não restringe a criação de categorias livres."
    )
    @ApiResponse(responseCode = "200", description = "Catálogo retornado com sucesso")
    public ResponseEntity<List<CategoryOptionDTO>> listCategories(
            @RequestParam(required = false) @Parameter(description = "Filtrar sugestões e histórico por tipo (INCOME ou EXPENSE)", example = "EXPENSE") TransactionType type,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(categoryService.listCategories(user.getId(), type));
    }
}
