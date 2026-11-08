package com.cement.prefacture.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Représente une ligne du "Requêteur BC" (Bon de Commande) exporté
 * depuis Sage — DES DONNÉES DE RÉFÉRENCE, pas des factures.
 *
 * Utilité : quand une Facture arrive (par scan OCR ou import), on
 * vérifie ici si son numéro de bon de commande correspond à un BC
 * connu, et si son statut est cohérent (reçu, signé, pas déjà
 * facturé) — voir BonCommandeService pour la logique de vérification.
 *
 * Colonnes réelles du fichier Sage "Requêteur_bc.xlsx" (confirmées
 * par lecture directe du fichier fourni) :
 *   Date commande | No commande | Description | Total HT lignes |
 *   Devise | Cours | Type de ligne | Service demandeur | Reçue |
 *   Signée | Fournisseur | Facturée | Raison sociale
 */
@Entity
@Table(name = "bon_commande", uniqueConstraints = @UniqueConstraint(columnNames = "numero_commande"))
@Getter
@Setter
public class BonCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_commande", nullable = false, unique = true)
    private String numeroCommande; // ex: "BCSCG01250879"

    @Column(name = "date_commande")
    private LocalDate dateCommande;

    private String description;

    @Column(name = "total_ht", precision = 14, scale = 3)
    private BigDecimal totalHT;

    @Column(length = 8)
    private String devise;

    private BigDecimal cours;

    @Column(name = "type_ligne")
    private String typeLigne; // ex: "STDSERVICE", "ITEM"

    @Column(name = "service_demandeur")
    private String serviceDemandeur; // ex: "BM", "IF"

    /** Colonne "Reçue" du fichier Sage : true si valeur = "Totalement" ou "Oui". */
    @Column(name = "recue")
    private boolean recue;

    /** Colonne "Signée" du fichier Sage — conservée en texte brut car
     *  Sage utilise plusieurs valeurs possibles ("Non", "Totalement",
     *  potentiellement "Partiellement") — un booléen perdrait cette nuance. */
    @Column(name = "statut_signature")
    private String statutSignature;

    @Column(name = "fournisseur_code")
    private String fournisseurCode; // ex: "FL-0038"

    /** Colonne "Facturée" du fichier Sage — true si ce BC a déjà une
     *  facture associée côté Sage. Sert à repérer les doublons : si une
     *  nouvelle facture arrive pour un BC déjà marqué facturé, c'est
     *  suspect (double facturation potentielle). */
    @Column(name = "facturee")
    private boolean facturee;

    @Column(name = "raison_sociale")
    private String raisonSociale; // nom complet du fournisseur
}