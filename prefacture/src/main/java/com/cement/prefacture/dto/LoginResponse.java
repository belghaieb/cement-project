package com.cement.prefacture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Ce qu'on renvoie au frontend après une connexion réussie.
 *
 * Remarquez ce qui N'EST PAS ici : pas de mot de passe, même chiffré,
 * et pas d'id technique. On ne renvoie que ce dont le frontend a
 * vraiment besoin : afficher le nom de l'utilisateur et adapter
 * l'interface selon son rôle (ex: cacher les boutons d'action pour IT,
 * comme déjà fait dans facture.html avec "document.body.dataset.role").
 *
 * @AllArgsConstructor (Lombok) génère un constructeur avec les 3 champs
 * dans l'ordre — c'est ce qui permet d'écrire directement
 * "new LoginResponse(username, nomComplet, role)" dans AuthController.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String nomComplet;
    private String role;
}