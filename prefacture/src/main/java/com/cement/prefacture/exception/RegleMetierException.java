package com.cement.prefacture.exception;

/**
 * À lever quand une action est TECHNIQUEMENT possible mais viole une
 * règle du métier (pas de la sécurité, pas des données manquantes —
 * juste une logique "ça n'a pas de sens de faire ça").
 *
 * Exemples concrets où on l'utilisera dans FactureService :
 *   - essayer de valider une facture déjà REJETEE
 *   - essayer de rejeter une facture déjà VALIDEE
 *   - essayer d'importer Sage alors qu'un import est déjà en cours
 *
 * On renvoie HTTP 409 (Conflict) pour cette exception dans
 * GestionnaireExceptionsGlobal — différent de 400 (donnée invalide)
 * ou 404 (introuvable) : ici la donnée existe et est valide, c'est
 * l'ACTION demandée qui est en conflit avec l'état actuel.
 */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) {
        super(message);
    }
}