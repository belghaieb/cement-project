package com.cement.prefacture.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Résumé d'une exécution d'import Sage — ce qu'on renvoie/log après
 * chaque passage, pour savoir exactement ce qui s'est passé sans
 * devoir éplucher les logs ligne par ligne.
 */
@Getter
@Setter
public class ResumeImport {
    private int inserees = 0;
    private int misesAJour = 0;
    private int ignorees = 0;
    private List<String> erreurs = new ArrayList<>();

    public void incrementerInserees() { inserees++; }
    public void incrementerMisesAJour() { misesAJour++; }
    public void incrementerIgnorees() { ignorees++; }
    public void ajouterErreur(String message) { erreurs.add(message); }
}