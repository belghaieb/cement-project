package com.cement.prefacture.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UtilisateurDetailsService utilisateurDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // ==========================
                        // AUTHENTIFICATION
                        // ==========================
                        .requestMatchers("/auth/**", "/h2-console/**").permitAll()

                        // ==========================
                        // SWAGGER
                        // ==========================
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ==========================
                        // LECTURE DES FACTURES
                        // ==========================
                        .requestMatchers(HttpMethod.GET, "/factures/**")
                        .hasAnyRole(
                                "ADMIN",
                                "RESPONSABLE_FACTURE",
                                "IT"
                        )

                        // ==========================
                        // VALIDATION / REJET
                        // ==========================
                        // IT peut désormais valider ou rejeter une
                        // facture, en plus du Responsable Facture et de
                        // l'Admin — déclaré AVANT la règle générale
                        // POST /factures/** ci-dessous, sinon cette
                        // règle plus large bloquerait IT en premier.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/factures/*/valider"
                        )
                        .hasAnyRole("IT", "RESPONSABLE_FACTURE", "ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/factures/*/rejeter"
                        )
                        .hasAnyRole("IT", "RESPONSABLE_FACTURE", "ADMIN")

                        // ==========================
                        // CONFIRMATION RECEPTION
                        // ==========================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/factures/*/confirmer-reception"
                        )
                        .hasAnyRole("IT", "ADMIN")

                        // ==========================
                        // CREATION FACTURE
                        // ==========================
                        .requestMatchers(HttpMethod.POST, "/factures/**")
                        .hasAnyRole(
                                "ADMIN",
                                "RESPONSABLE_FACTURE"
                        )

                        // ==========================
                        // MODIFICATION FACTURE
                        // ==========================
                        .requestMatchers(HttpMethod.PUT, "/factures/**")
                        .hasAnyRole(
                                "ADMIN",
                                "RESPONSABLE_FACTURE"
                        )

                        // ==========================
                        // SUPPRESSION FACTURE
                        // ==========================
                        // Seul le Responsable Facture
                        // peut supprimer une facture.
                        .requestMatchers(HttpMethod.DELETE, "/factures/**")
                        .hasRole("RESPONSABLE_FACTURE")

                        // ==========================
                        // IMPORT SAGE
                        // ==========================
                        .requestMatchers("/import/**")
                        .hasRole("ADMIN")

                        // ==========================
                        // ADMINISTRATION
                        // ==========================
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // ==========================
                        // AUTRES
                        // ==========================
                        .anyRequest()
                        .authenticated()

                )

                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(utilisateurDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }
}