package com.cement.prefacture.exception;

/**
 * À lever quand le moteur OCR (Tesseract) échoue techniquement —
 * pas "confiance basse" (ça, ce n'est pas une erreur, juste un
 * résultat imparfait qu'on gère via ResultatOcr.necessiteVerification),
 * mais un vrai échec : Tesseract non installé, chemin tessdata
 * introuvable, image illisible, etc.
 *
 * Le constructeur prend une "cause" (Throwable) en plus du message :
 * ça préserve la TesseractException originale à l'intérieur, pour
 * qu'on puisse voir la vraie erreur technique dans les logs (via
 * log.error(...) dans GestionnaireExceptionsGlobal) même si le
 * message renvoyé au frontend reste simple et sans détail technique.
 */
public class OcrEchecException extends RuntimeException {
    public OcrEchecException(String message, Throwable cause) {
        super(message, cause);
    }
}