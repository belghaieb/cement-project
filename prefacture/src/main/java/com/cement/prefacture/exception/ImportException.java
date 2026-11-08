package com.cement.prefacture.exception;

/**
 * À lever quand l'import automatique depuis le fichier Sage échoue :
 * fichier introuvable au chemin configuré, format .xlsx illisible/
 * corrompu, colonnes attendues absentes, etc.
 *
 * Utilisée par le futur ImportService, notamment dans la tâche
 * planifiée (@Scheduled) qui tourne toutes les 8h — si cette tâche
 * échoue, on veut une trace claire dans les logs plutôt qu'un échec
 * silencieux qui ferait juste "disparaître" l'import sans explication.
 */
public class ImportException extends RuntimeException {
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}