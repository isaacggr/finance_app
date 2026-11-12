package com.isaacggr.finance_app.config;

import com.isaacggr.finance_app.controller.AuthController;
import com.isaacggr.finance_app.controller.UserController;
import com.isaacggr.finance_app.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    @Bean
    public OpenAPI customOpenAPI() {
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        ResolvedSchema errorResolvedSchema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(ErrorResponse.class);
        if (errorResolvedSchema != null) {
            if (errorResolvedSchema.schema != null) {
                components.addSchemas("ErrorResponse", errorResolvedSchema.schema);
            }
            if (errorResolvedSchema.referencedSchemas != null) {
                errorResolvedSchema.referencedSchemas.forEach(components::addSchemas);
            }
        }

        return new OpenAPI()
                .info(new Info()
                        .title("💰 Finance App API")
                        .description("### API RESTful para controle e gestão financeira pessoal\n\n"
                                + "Para testar rotas protegidas:\n"
                                + "1. Crie sua conta em `POST /users` (ou use um usuário existente);\n"
                                + "2. Autentique-se em `POST /auth/login` e copie o `token`;\n"
                                + "3. Clique no botão **Authorize 🔓** no topo e informe o token JWT.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Isaac Gregório")
                                .url("https://github.com/isaacggr")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(components);
    }

    /**
     * Padroniza automaticamente no contrato OpenAPI as respostas transversais de validação (400)
     * e autenticação JWT (401), removendo exigência de cadeado nas rotas públicas.
     */
    @Bean
    public OperationCustomizer standardErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            if (isPublicEndpoint(handlerMethod)) {
                operation.setSecurity(List.of());
                addErrorResponseIfAbsent(
                        operation,
                        "429",
                        "Limite de requisições excedido para proteção contra força bruta/abuso. Aguarde o tempo indicado no header Retry-After."
                );
            } else {
                addErrorResponseIfAbsent(
                        operation,
                        "401",
                        "Autenticação necessária. Token JWT ausente, inválido ou expirado."
                );
            }

            if (hasRequestInputs(handlerMethod)) {
                addErrorResponseIfAbsent(
                        operation,
                        "400",
                        "Dados inválidos, erro de validação nos campos ou violação de regra de negócio."
                );
            }

            return operation;
        };
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        Class<?> controller = handlerMethod.getBeanType();
        if (controller.equals(AuthController.class)) {
            return true;
        }
        return controller.equals(UserController.class)
                && "create".equals(handlerMethod.getMethod().getName());
    }

    private boolean hasRequestInputs(HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(RequestBody.class)
                    || parameter.hasParameterAnnotation(RequestParam.class)
                    || parameter.hasParameterAnnotation(PathVariable.class)
                    || Pageable.class.isAssignableFrom(parameter.getParameterType())) {
                return true;
            }
        }
        return false;
    }

    private void addErrorResponseIfAbsent(Operation operation, String statusCode, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(statusCode)) {
            return;
        }

        ApiResponse errorResponse = new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                ));
        responses.addApiResponse(statusCode, errorResponse);
    }
}
