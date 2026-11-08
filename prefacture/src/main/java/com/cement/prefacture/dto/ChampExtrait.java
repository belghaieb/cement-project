package com.cement.prefacture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente UN champ extrait par l'OCR, avec son niveau de confiance.
 * Exemple : nom = "montantHT", valeur = "12 340,500", confiance = 74.
 *
 * C'est exactement ce que l'écran de révision (facture.html) affiche
 * dans le panneau de droite : chaque <input> avec son badge de
 * confiance (confidence--high / confidence--med / confidence--low)
 * vient d'un objet ChampExtrait.
 */
@Getter
@Setter
@AllArgsConstructor
public class ChampExtrait {
    private String nom;
    private String valeur;
    private int confiance; // 0 à 100

    /**
     * Résumé d'une exécution d'import Sage — ce qu'on renvoie/log après
     * chaque passage, pour savoir exactement ce qui s'est passé sans
     * devoir éplucher les logs ligne par ligne.
     */
    @Getter
    @Setter
    public static class ResumeImport {
        private int inserees = 0;
        private int misesAJour = 0;
        private int ignorees = 0;
        private List<String> erreurs = new ArrayList<>();

        public void incrementerInserees() { inserees++; }
        public void incrementerMisesAJour() { misesAJour++; }
        public void incrementerIgnorees() { ignorees++; }
        public void ajouterErreur(String message) { erreurs.add(message); }
    }
}