package com.cement.prefacture.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Format STANDARD renvoyé au frontend chaque fois qu'une erreur se
 * produit, peu importe laquelle. Au lieu que chaque erreur ait sa
 * propre forme de réponse JSON (imprévisible pour le frontend), toutes
 * passent par cette même structure.
 *
 * Exemple de réponse générée à partir de cette classe :
 * {
 *   "horodatage": "2026-07-28T14:32:10",
 *   "statut": 404,
 *   "erreur": "Not Found",
 *   "message": "Facture introuvable avec l'id : 42",
 *   "chemin": "/factures/42"
 * }
 *
 * Le frontend peut donc TOUJOURS s'attendre à cette forme, et afficher
 * "message" directement à l'utilisateur sans deviner la structure.
 */
@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime horodatage = LocalDateTime.now();
    private int statut;
    private String erreur;
    private String message;
    private String chemin; // l'URL qui a provoqué l'erreur, utile pour déboguer
}