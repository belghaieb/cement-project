package com.cement.prefacture.dto;

import com.cement.prefacture.entity.Facture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Réponse renvoyée après un scan : la Facture créée, ACCOMPAGNÉE des
 * alertes de cohérence détectées par rapport au registre des Bons de
 * Commande (voir BonCommandeService).
 *
 * On ne bloque JAMAIS la création de la facture à cause d'une alerte —
 * une facture avec un BC inconnu ou déjà facturé est quand même créée,
 * mais l'humain voit immédiatement le problème à l'écran de révision
 * plutôt que de le découvrir après validation.
 */
@Getter
@Setter
@AllArgsConstructor
public class FactureAvecAlertes {
    private Facture facture;
    private List<String> alertesBonCommande;
}