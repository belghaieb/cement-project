package com.cement.prefacture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de la vérification d'une Facture contre le registre des
 * Bons de Commande (BonCommande) — voir BonCommandeService.
 *
 * "alertes" contient une liste de problèmes détectés (BC inconnu,
 * pas encore reçu, déjà facturé...). Une liste VIDE = tout est
 * cohérent, aucune alerte.
 */
@Getter
@Setter
public class ResultatVerificationBc {
    private boolean bcTrouve;
    private List<String> alertes = new ArrayList<>();

    public void ajouterAlerte(String message) {
        alertes.add(message);
    }

    public boolean estCoherent() {
        return bcTrouve && alertes.isEmpty();
    }
}