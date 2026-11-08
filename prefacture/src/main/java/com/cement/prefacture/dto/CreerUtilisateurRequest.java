package com.cement.prefacture.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Ce que l'Admin envoie pour créer un nouveau compte utilisateur.
 */
@Getter
@Setter
public class CreerUtilisateurRequest {
    private String username;
    private String password;
    private String nomComplet;
    private String role; // "ADMIN" | "RESPONSABLE_FACTURE" | "IT"
}