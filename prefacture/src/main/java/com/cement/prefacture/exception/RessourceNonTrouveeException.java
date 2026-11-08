package com.cement.prefacture.exception;

/**
 * À lever quand on cherche quelque chose qui n'existe pas en base :
 * une facture avec un id inconnu, une photo introuvable, etc.
 *
 * On étend RuntimeException (et non Exception) pour ne PAS être obligé
 * d'écrire "throws RessourceNonTrouveeException" sur chaque méthode qui
 * pourrait la lever — GestionnaireExceptionsGlobal l'attrape
 * automatiquement, peu importe où elle est levée dans le code.
 *
 * Exemple d'utilisation (dans un futur FactureService) :
 *   factureRepository.findById(id)
 *       .orElseThrow(() -> new RessourceNonTrouveeException("Facture introuvable : " + id));
 */
public class RessourceNonTrouveeException extends RuntimeException {
    public RessourceNonTrouveeException(String message) {
        super(message);
    }
}