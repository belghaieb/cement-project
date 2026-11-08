package com.cement.prefacture.exception;

/**
 * À lever quand un fichier envoyé par le client ne convient pas :
 * mauvais type (ex: un .pdf envoyé comme photo), fichier vide,
 * ou fichier corrompu qu'on n'arrive pas à lire.
 *
 * Utilisée dans PhotoService (à venir) avant même d'essayer de
 * sauvegarder le fichier sur le disque — mieux vaut rejeter tôt
 * plutôt que de découvrir le problème après écriture.
 */
public class FichierInvalideException extends RuntimeException {
    public FichierInvalideException(String message) {
        super(message);
    }
}