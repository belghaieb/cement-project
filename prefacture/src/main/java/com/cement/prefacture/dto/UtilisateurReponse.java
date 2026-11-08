package com.cement.prefacture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Ce qu'on renvoie quand on liste/crée des utilisateurs — jamais le
 * mot de passe (même chiffré), même à l'Admin.
 */
@Getter
@Setter
@AllArgsConstructor
public class UtilisateurReponse {
    private Long id;
    private String username;
    private String nomComplet;
    private String role;
    private boolean actif;
}