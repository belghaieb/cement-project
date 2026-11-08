package com.cement.prefacture.controller;

import com.cement.prefacture.dto.CreerUtilisateurRequest;
import com.cement.prefacture.dto.UtilisateurReponse;
import com.cement.prefacture.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des comptes utilisateurs — c'est LE rôle central de l'Admin
 * dans l'application : créer les comptes Responsable Facture et IT,
 * les désactiver si besoin. Toutes les routes sont sous /admin/**,
 * déjà réservées à ADMIN dans SecurityConfig.
 */
@RestController
@RequestMapping("/admin/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PostMapping
    public ResponseEntity<UtilisateurReponse> creer(@RequestBody CreerUtilisateurRequest requete) {
        return ResponseEntity.ok(utilisateurService.creer(requete));
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurReponse>> listerTous() {
        return ResponseEntity.ok(utilisateurService.listerTous());
    }

    @PutMapping("/{id}/desactiver")
    public ResponseEntity<UtilisateurReponse> desactiver(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.desactiver(id));
    }
}