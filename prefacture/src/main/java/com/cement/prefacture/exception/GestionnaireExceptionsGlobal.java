package com.cement.prefacture.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Point d'interception UNIQUE pour toutes les erreurs de l'application.
 *
 * @RestControllerAdvice fait que Spring appelle automatiquement les
 * méthodes ci-dessous dès qu'une exception correspondante est levée
 * N'IMPORTE OÙ dans un contrôleur — pas besoin d'un try/catch répété
 * dans chaque endpoint de FactureController, PhotoController, etc.
 *
 * Chaque méthode @ExceptionHandler dit : "si CETTE exception apparaît,
 * renvoie CE code HTTP avec CE message."
 */
@RestControllerAdvice
public class GestionnaireExceptionsGlobal {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GestionnaireExceptionsGlobal.class);

    // ================= Exceptions métier personnalisées =================

    @ExceptionHandler(RessourceNonTrouveeException.class)
    public ResponseEntity<ErrorResponse> gererRessourceNonTrouvee(RessourceNonTrouveeException ex, HttpServletRequest req) {
        return construireReponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(FichierInvalideException.class)
    public ResponseEntity<ErrorResponse> gererFichierInvalide(FichierInvalideException ex, HttpServletRequest req) {
        return construireReponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<ErrorResponse> gererRegleMetier(RegleMetierException ex, HttpServletRequest req) {
        return construireReponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(OcrEchecException.class)
    public ResponseEntity<ErrorResponse> gererOcrEchec(OcrEchecException ex, HttpServletRequest req) {
        // On logue la cause technique complète (ex: TesseractException),
        // mais on renvoie un message simple au frontend — l'utilisateur
        // n'a pas besoin de voir une stack trace Java.
        log.error("Échec OCR sur {} : ", req.getRequestURI(), ex);
        return construireReponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Échec du traitement OCR : " + ex.getMessage(), req);
    }

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ErrorResponse> gererImportException(ImportException ex, HttpServletRequest req) {
        log.error("Échec import Sage : ", ex);
        return construireReponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Échec de l'import : " + ex.getMessage(), req);
    }

    // ================= Exceptions Spring / framework =================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> gererIdentifiantsInvalides(BadCredentialsException ex, HttpServletRequest req) {
        // Message volontairement vague : ne précise jamais si c'est le
        // username ou le mot de passe qui est incorrect (bonne pratique
        // de sécurité, évite de faciliter le "user enumeration").
        return construireReponse(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect.", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> gererAccesRefuse(AccessDeniedException ex, HttpServletRequest req) {
        // Levée automatiquement par SecurityConfig quand un rôle
        // n'a pas la permission (ex: IT qui tente un POST /factures).
        return construireReponse(HttpStatus.FORBIDDEN, "Accès refusé : rôle insuffisant pour cette action.", req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> gererFichierTropVolumineux(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        // Déclenchée automatiquement si un fichier dépasse
        // spring.servlet.multipart.max-file-size (10MB, voir application.properties)
        return construireReponse(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux (max 10MB).", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> gererViolationIntegrite(DataIntegrityViolationException ex, HttpServletRequest req) {
        // Déclenchée par MySQL/H2 quand une contrainte est violée —
        // typiquement notre contrainte unique sur numero_facture.
        // C'est le "filet de sécurité" final si jamais ImportService
        // avait un bug et essayait quand même d'insérer un doublon.
        return construireReponse(HttpStatus.CONFLICT,
                "Conflit de données : une entrée avec cette référence existe déjà.", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> gererValidationEchouee(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // Déclenchée quand une annotation @Valid (Bean Validation)
        // détecte un champ invalide dans le corps JSON reçu.
        String messages = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));
        return construireReponse(HttpStatus.BAD_REQUEST, messages, req);
    }

    // ================= Filet de sécurité : tout le reste =================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> gererExceptionGenerique(Exception ex, HttpServletRequest req) {
        // Attrape TOUT ce qui n'a pas de handler spécifique ci-dessus.
        // On logue la stack trace complète pour pouvoir déboguer,
        // mais on ne renvoie jamais le détail technique au frontend —
        // exposer une stack trace à l'utilisateur final serait à la
        // fois inutile pour lui et risqué en termes de sécurité.
        log.error("Erreur non gérée sur {} : ", req.getRequestURI(), ex);
        return construireReponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur inattendue est survenue. Contactez l'administrateur si cela persiste.", req);
    }

    /**
     * Méthode utilitaire partagée par tous les handlers ci-dessus,
     * pour éviter de reconstruire un ErrorResponse à la main à chaque
     * fois — un seul endroit à modifier si le format doit changer.
     */
    private ResponseEntity<ErrorResponse> construireReponse(HttpStatus statut, String message, HttpServletRequest req) {
        ErrorResponse erreur = new ErrorResponse(
                java.time.LocalDateTime.now(),
                statut.value(),
                statut.getReasonPhrase(),
                message,
                req.getRequestURI()
        );
        return ResponseEntity.status(statut).body(erreur);
    }
}