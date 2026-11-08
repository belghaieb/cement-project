package com.cement.prefacture.controller;

import com.cement.prefacture.dto.LoginRequest;
import com.cement.prefacture.dto.LoginResponse;
import com.cement.prefacture.entity.Utilisateur;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Persist the session so subsequent requests stay authenticated
        httpRequest.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        Utilisateur utilisateur = (Utilisateur) auth.getPrincipal();

        LoginResponse response = new LoginResponse(
                utilisateur.getUsername(),
                utilisateur.getNomComplet(),
                utilisateur.getRole().name()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Spring Security a TOUJOURS un objet Authentication, même pour
        // un visiteur non connecté — c'est un "AnonymousAuthenticationToken",
        // pas null. Sans cette vérification supplémentaire, le code
        // essayait de caster ce principal anonyme (une simple chaîne
        // "anonymousUser") en Utilisateur, ce qui provoquait un
        // ClassCastException → une erreur 500 au lieu d'un simple 401.
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }

        Utilisateur utilisateur = (Utilisateur) auth.getPrincipal();
        LoginResponse response = new LoginResponse(
                utilisateur.getUsername(),
                utilisateur.getNomComplet(),
                utilisateur.getRole().name()
        );
        return ResponseEntity.ok(response);
    }
}