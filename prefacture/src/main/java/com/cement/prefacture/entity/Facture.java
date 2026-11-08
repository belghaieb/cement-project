package com.cement.prefacture.entity;

import com.cement.prefacture.enums.StatutFacture;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représente une facture fournisseur suivie dans l'application.
 *
 * Chaque ligne de cette table correspond à UNE facture reçue par
 * l'entreprise (via scan/photo ou via l'import Sage). C'est l'entité
 * centrale de tout le projet — presque tout le reste (photos, OCR,
 * validation) tourne autour d'un objet Facture.
 */
@Entity
// La contrainte "unique" sur numero_facture empêche d'avoir deux fois
// la même facture en base — c'est ce qui rend l'import Sage "sûr" :
// si on réimporte le même fichier deux fois, MySQL refusera les doublons
// même si notre code applicatif avait un bug.
@Table(name = "facture", uniqueConstraints = @UniqueConstraint(columnNames = "numero_facture"))
@Getter
@Setter
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence unique de la facture (ex: "FA-2026-01846").
     * C'est LA clé qu'on utilise pour savoir si une facture importée
     * depuis Sage existe déjà en base (voir ImportService, à venir).
     */
    @Column(name = "numero_facture", unique = true)
    private String numeroFacture;

    @Column
    private String fournisseur;

    /** Matricule fiscal du fournisseur (obligatoire en Tunisie). */
    @Column(name = "mf_fournisseur")
    private String mfFournisseur;

    private LocalDate dateFacture;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "bon_de_commande")
    private String bonDeCommande;

    /**
     * precision/scale contrôlent combien de chiffres MySQL réserve.
     * 14 chiffres au total, dont 3 après la virgule — largement
     * suffisant pour des montants en TND (ex: 12340,500).
     * On utilise BigDecimal (pas double/float) car les montants
     * financiers ne doivent JAMAIS subir d'erreurs d'arrondi binaire.
     */
    @Column(name = "montant_ht", precision = 14, scale = 3)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", precision = 14, scale = 3)
    private BigDecimal montantTVA;

    @Column(name = "montant_ttc", precision = 14, scale = 3)
    private BigDecimal montantTTC;

    @Column(length = 8)
    private String devise = "TND";

    /**
     * Statut du cycle de vie de la facture. Toute facture démarre à
     * A_VERIFIER par défaut — c'est FactureService qui la fera avancer
     * vers EN_COURS, VALIDEE ou REJETEE selon les actions humaines.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFacture statut = StatutFacture.A_VERIFIER;

    /**
     * Score de confiance moyen du dernier passage OCR (0-100).
     * Rempli par OcrService — utilisé pour décider si la facture
     * doit être vérifiée manuellement avant validation.
     */
    @Column(name = "confiance_moyenne")
    private Integer confianceMoyenne;

    /** Date à laquelle la facture est entrée dans le système. */
    @Column(name = "date_reception", nullable = false)
    private LocalDateTime dateReception = LocalDateTime.now();

    /** Username de la personne qui a validé/rejeté — trace d'audit. */
    @Column(name = "valide_par")
    private String validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    /**
     * Une facture peut avoir plusieurs photos (preuve de réception).
     * mappedBy = "facture" : c'est PhotoReception qui possède la clé
     * étrangère (facture_id), pas cette table — Facture ne fait que
     * "voir" la liste, elle ne stocke pas de colonne pour ça.
     *
     * cascade = ALL : si on supprime une Facture, ses photos associées
     * sont supprimées avec elle (pas de photos orphelines en base).
     * orphanRemoval = true : si on retire une photo de cette liste en
     * Java, elle est aussi supprimée en base (pas juste "détachée").
     */
    @Column(name = "chemin_image_scan")
    private String cheminImageScan;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "reception_confirmee")
    private boolean receptionConfirmee = false;

    @Column(name = "confirmee_par")
    private String confirmeePar; // username de l'IT qui a confirmé

    @Column(name = "date_confirmation_reception")
    private LocalDateTime dateConfirmationReception;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoReception> photos;

    /**
     * Nombre de jours écoulés depuis la réception de la facture —
     * calculé à la volée, jamais stocké en base (une valeur stockée
     * deviendrait fausse le jour suivant sans mise à jour manuelle).
     * @Transient : Hibernate ignore ce champ, pas de colonne créée,
     * mais Jackson le sérialise quand même dans la réponse JSON car
     * c'est un getter normal.
     */
    @jakarta.persistence.Transient
    public long getAgeJours() {
        if (dateReception == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dateReception.toLocalDate(), LocalDate.now());
    }
}