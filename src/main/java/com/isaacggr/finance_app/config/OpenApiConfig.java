package com.isaacggr.finance_app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("💰 Finance App API")
                        .description("### API RESTful para controle e gestão financeira pessoal\n\n"
                                + "Para testar rotas protegidas:\n"
                                + "1. Crie sua conta em `POST /users` (ou use um usuário existente);\n"
                                + "2. Autentique-se em `POST /auth/login` e copie o `accessToken`;\n"
                                + "3. Clique no botão **Authorize 🔓** no topo, informe `Bearer SEU_TOKEN` e confirme.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Isaac Gregório")
                                .url("https://github.com/isaacggr")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
