package com.cement.prefacture.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Regroupe le résultat complet d'un passage OCR sur une image.
 *
 * C'est ce que OcrService.analyser() renvoie, et ce qu'un futur
 * FactureController utilisera pour créer/mettre à jour une Facture
 * après un scan.
 *
 * Ce n'est PAS la même chose qu'une Facture : ResultatOcr est une
 * donnée "brute", temporaire, issue directement du scan — elle n'est
 * pas encore stockée en base. C'est FactureService qui décidera quoi
 * en faire (créer une nouvelle Facture, ou juste afficher les champs
 * pour vérification avant de les enregistrer).
 */
@Getter
@Setter
public class ResultatOcr {

    /** Texte complet et brut renvoyé par Tesseract, avant tout découpage. */
    private String texteBrut;

    /** Liste des champs identifiés (numéro facture, date, montant...). */
    private List<ChampExtrait> champs;

    /** Confiance moyenne globale du scan (0-100). */
    private int confianceMoyenne;

    /**
     * true si la confiance est sous le seuil (voir OcrService.SEUIL_CONFIANCE) —
     * dans ce cas, le double passage avec Gemini/prétraitement a été
     * déclenché, et/ou la facture doit être marquée A_VERIFIER plutôt
     * que d'être acceptée automatiquement.
     */
    private boolean necessiteVerification;
}