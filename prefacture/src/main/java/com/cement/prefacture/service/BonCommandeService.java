package com.cement.prefacture.service;

import com.cement.prefacture.dto.ResultatVerificationBc;
import com.cement.prefacture.entity.BonCommande;
import com.cement.prefacture.repository.BonCommandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Vérifie la cohérence d'une facture par rapport au registre des
 * Bons de Commande (référence importée depuis Sage, voir ImportService
 * et BonCommande.java).
 *
 * BUT CONCRET : quand une facture arrive (scannée ou saisie), on
 * vérifie que son numéro de BC existe réellement, qu'il a bien été
 * reçu/signé, et qu'il n'a pas déjà été facturé ailleurs — ça permet
 * de repérer une facture suspecte AVANT validation, plutôt qu'après.
 */
@Service
@RequiredArgsConstructor
public class BonCommandeService {

    private final BonCommandeRepository bonCommandeRepository;

    public ResultatVerificationBc verifier(String numeroCommande) {
        ResultatVerificationBc resultat = new ResultatVerificationBc();

        if (numeroCommande == null || numeroCommande.isBlank()) {
            resultat.setBcTrouve(false);
            resultat.ajouterAlerte("Aucun numéro de bon de commande fourni sur la facture.");
            return resultat;
        }

        Optional<BonCommande> bcOpt = bonCommandeRepository.findByNumeroCommande(numeroCommande);

        if (bcOpt.isEmpty()) {
            resultat.setBcTrouve(false);
            resultat.ajouterAlerte("Bon de commande " + numeroCommande + " introuvable dans le registre Sage.");
            return resultat;
        }

        resultat.setBcTrouve(true);
        BonCommande bc = bcOpt.get();

        if (!bc.isRecue()) {
            resultat.ajouterAlerte("Ce bon de commande n'est pas encore marqué comme reçu dans Sage.");
        }

        if (bc.getStatutSignature() != null && bc.getStatutSignature().equalsIgnoreCase("Non")) {
            resultat.ajouterAlerte("Ce bon de commande n'a pas encore été signé.");
        }

        if (bc.isFacturee()) {
            // Alerte la plus importante : risque de double facturation.
            resultat.ajouterAlerte("⚠ Ce bon de commande est déjà marqué comme facturé dans Sage — vérifier un possible doublon.");
        }

        return resultat;
    }
}