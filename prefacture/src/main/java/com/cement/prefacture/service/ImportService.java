package com.cement.prefacture.service;

import com.cement.prefacture.dto.ResumeImport;
import com.cement.prefacture.entity.BonCommande;
import com.cement.prefacture.exception.ImportException;
import com.cement.prefacture.repository.BonCommandeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Importe le registre des Bons de Commande depuis le fichier Sage
 * "Requêteur_bc.xlsx", toutes les 8 heures — ET une fois au démarrage
 * de l'application (voir implements ApplicationRunner ci-dessous).
 *
 * CORRECTION IMPORTANTE : ce fichier n'est PAS un registre de
 * factures — c'est un registre de BONS DE COMMANDE, utilisé comme
 * DONNÉES DE RÉFÉRENCE pour vérifier la cohérence des factures
 * scannées (voir BonCommandeService). On importe donc vers la table
 * bon_commande, pas vers facture.
 *
 * Colonnes réelles confirmées (Sheet1, ligne d'en-tête à la ligne 0) :
 *   0. Date commande     5. Cours              10. Fournisseur
 *   1. No commande       6. Type de ligne      11. Facturée
 *   2. Description       7. Service demandeur  12. Raison sociale
 *   3. Total HT lignes   8. Reçue
 *   4. Devise             9. Signée
 *
 * RÈGLE D'UPSERT : contrairement aux factures, un Bon de Commande
 * n'a pas de "statut validé par un humain" à préserver — on écrase
 * donc TOUJOURS avec les données Sage les plus récentes, sans
 * exception. C'est cohérent : ce ne sont que des données de
 * référence, personne ne les corrige manuellement dans notre appli.
 *
 * IMPORT AU DÉMARRAGE : en plus du cron toutes les 8h, la classe
 * implémente ApplicationRunner — Spring Boot appelle automatiquement
 * la méthode run() juste après le démarrage complet de l'application,
 * AVANT que la moindre requête HTTP ne soit traitée. Cela ne passe
 * PAS par le contrôleur ni par Spring Security : aucune connexion,
 * aucun rôle ADMIN n'est nécessaire, exactement comme le job planifié.
 * On réutilise directement importerAutomatiquement() plutôt que de
 * dupliquer la logique, pour que les deux déclencheurs (démarrage et
 * cron) respectent toujours exactement le même comportement — y
 * compris la vérification du flag app.sage-import.enabled.
 */
@Service
@RequiredArgsConstructor
public class ImportService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final BonCommandeRepository bonCommandeRepository;

    @Value("${app.sage-import.path}")
    private String cheminFichierSage;

    @Value("${app.sage-import.enabled}")
    private boolean importActif;

    private static final int COLONNE_DATE_COMMANDE = 0;
    private static final int COLONNE_NO_COMMANDE = 1;
    private static final int COLONNE_DESCRIPTION = 2;
    private static final int COLONNE_TOTAL_HT = 3;
    private static final int COLONNE_DEVISE = 4;
    private static final int COLONNE_COURS = 5;
    private static final int COLONNE_TYPE_LIGNE = 6;
    private static final int COLONNE_SERVICE_DEMANDEUR = 7;
    private static final int COLONNE_RECUE = 8;
    private static final int COLONNE_SIGNEE = 9;
    private static final int COLONNE_FOURNISSEUR = 10;
    private static final int COLONNE_FACTUREE = 11;
    private static final int COLONNE_RAISON_SOCIALE = 12;

    /**
     * Appelée automatiquement UNE FOIS par Spring Boot, juste après le
     * démarrage complet de l'application (contexte chargé, serveur web
     * prêt). C'est le mécanisme demandé : importer dès le lancement de
     * l'app, sans qu'un utilisateur ait besoin de se connecter ou de
     * déclencher quoi que ce soit manuellement.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!importActif) {
            log.info("Import Sage au démarrage ignoré (app.sage-import.enabled=false)");
            return;
        }
        log.info("Import Sage au démarrage : lancement...");
        ResumeImport resume = importerDepuisFichier(new File(cheminFichierSage));
        log.info("Import Sage au démarrage terminé : {} insérées, {} mises à jour, {} ignorée(s), {} erreur(s)",
                resume.getInserees(), resume.getMisesAJour(), resume.getIgnorees(), resume.getErreurs().size());
    }

    @Scheduled(cron = "${app.sage-import.cron}")
    public void importerAutomatiquement() {
        if (!importActif) {
            return;
        }
        importerDepuisFichier(new File(cheminFichierSage));
    }

    @Transactional
    public ResumeImport importerDepuisFichier(File fichierExcel) {
        ResumeImport resume = new ResumeImport();

        try (FileInputStream flux = new FileInputStream(fichierExcel);
             Workbook classeur = WorkbookFactory.create(flux)) {

            Sheet feuille = classeur.getSheetAt(0);

            for (int i = 1; i <= feuille.getLastRowNum(); i++) {
                Row ligne = feuille.getRow(i);
                if (ligne == null) continue;

                try {
                    traiterLigne(ligne, resume);
                } catch (Exception ex) {
                    resume.ajouterErreur("Ligne " + (i + 1) + " : " + ex.getMessage());
                }
            }

        } catch (IOException ex) {
            throw new ImportException("Impossible de lire le fichier Sage : " + fichierExcel.getPath(), ex);
        }

        return resume;
    }

    private void traiterLigne(Row ligne, ResumeImport resume) {
        String numeroCommande = lireTexte(ligne, COLONNE_NO_COMMANDE);
        if (numeroCommande == null || numeroCommande.isBlank()) {
            resume.ajouterErreur("Ligne " + (ligne.getRowNum() + 1) + " : numéro de commande manquant, ignorée.");
            return;
        }

        Optional<BonCommande> existant = bonCommandeRepository.findByNumeroCommande(numeroCommande);
        BonCommande bc = existant.orElseGet(BonCommande::new);
        boolean estNouveau = existant.isEmpty();

        bc.setNumeroCommande(numeroCommande);
        bc.setDateCommande(lireDate(ligne, COLONNE_DATE_COMMANDE));
        bc.setDescription(lireTexte(ligne, COLONNE_DESCRIPTION));
        bc.setTotalHT(lireMontant(ligne, COLONNE_TOTAL_HT));
        bc.setDevise(lireTexte(ligne, COLONNE_DEVISE));
        bc.setCours(lireMontant(ligne, COLONNE_COURS));
        bc.setTypeLigne(lireTexte(ligne, COLONNE_TYPE_LIGNE));
        bc.setServiceDemandeur(lireTexte(ligne, COLONNE_SERVICE_DEMANDEUR));
        bc.setRecue(interpreterOuiNon(lireTexte(ligne, COLONNE_RECUE)));
        bc.setStatutSignature(lireTexte(ligne, COLONNE_SIGNEE));
        bc.setFournisseurCode(lireTexte(ligne, COLONNE_FOURNISSEUR));
        bc.setFacturee(interpreterOuiNon(lireTexte(ligne, COLONNE_FACTUREE)));
        bc.setRaisonSociale(lireTexte(ligne, COLONNE_RAISON_SOCIALE));

        bonCommandeRepository.save(bc);

        if (estNouveau) {
            resume.incrementerInserees();
        } else {
            resume.incrementerMisesAJour();
        }
    }

    /**
     * Convertit les valeurs Sage ("Oui", "Totalement", "Non"...) en
     * booléen. "Totalement" et "Oui" comptent comme vrai ; tout le
     * reste ("Non", "Partiellement", vide) compte comme faux.
     */
    private boolean interpreterOuiNon(String valeur) {
        if (valeur == null) return false;
        String v = valeur.trim().toLowerCase();
        return v.equals("oui") || v.equals("totalement");
    }

    private String lireTexte(Row ligne, int colonne) {
        Cell cellule = ligne.getCell(colonne);
        if (cellule == null) return null;
        DataFormatter formateur = new DataFormatter();
        return formateur.formatCellValue(cellule).trim();
    }

    private LocalDate lireDate(Row ligne, int colonne) {
        Cell cellule = ligne.getCell(colonne);
        if (cellule == null) return null;
        try {
            if (DateUtil.isCellDateFormatted(cellule)) {
                return cellule.getLocalDateTimeCellValue().toLocalDate();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal lireMontant(Row ligne, int colonne) {
        Cell cellule = ligne.getCell(colonne);
        if (cellule == null) return null;
        try {
            return BigDecimal.valueOf(cellule.getNumericCellValue());
        } catch (Exception ex) {
            return null;
        }
    }
}