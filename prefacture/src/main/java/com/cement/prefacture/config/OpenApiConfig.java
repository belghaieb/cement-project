package com.cement.prefacture.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Personnalise la page Swagger UI (titre, description) — purement
 * cosmétique, mais rend la documentation plus claire pour toi et pour
 * quiconque tester l'API plus tard (Hassen, par exemple).
 *
 * Une fois l'app lancée, la doc est disponible sur :
 *   http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Préfacture API")
                        .version("1.0")
                        .description("API de gestion et traçabilité des factures — SCG"));
    }
}