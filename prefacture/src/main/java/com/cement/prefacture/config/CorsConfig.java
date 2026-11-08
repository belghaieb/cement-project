package com.cement.prefacture.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Autorise le frontend (servi sur un port différent, ex: localhost:8000)
 * à appeler l'API backend (localhost:8080).
 *
 * Sans cette configuration, le NAVIGATEUR bloque lui-même les requêtes
 * (erreur CORS visible dans la console JS) — ce n'est pas un problème
 * réseau, c'est une protection de sécurité intégrée aux navigateurs
 * qui empêche un site web d'appeler discrètement l'API d'un autre.
 *
 * allowCredentials(true) est ESSENTIEL ici : notre authentification est
 * basée sur des sessions (cookies), donc le navigateur doit être
 * explicitement autorisé à envoyer/recevoir ces cookies entre les
 * deux ports différents.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Adresses EXACTES du frontend — pas de wildcard "*" possible ici,
        // car allowCredentials(true) l'interdit explicitement (règle du
        // standard CORS, pas une limitation Spring).
        //
        // Les deux origines ci-dessous couvrent : Live Server de VS Code
        // (127.0.0.1:5500, port par défaut) ET localhost:8000 si jamais
        // un autre serveur statique est utilisé à la place.
        configuration.setAllowedOrigins(List.of(
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "http://localhost:8000"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}