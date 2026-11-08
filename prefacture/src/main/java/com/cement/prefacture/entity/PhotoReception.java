package com.cement.prefacture.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Représente une photo prise comme preuve de réception d'une facture
 * (ex: photo du bon de livraison, ou de la facture elle-même à son
 * arrivée physique à l'usine).
 *
 * IMPORTANT : cette table ne contient JAMAIS l'image elle-même.
 * Elle stocke seulement le CHEMIN vers le fichier, qui vit sur le
 * disque (ou plus tard un espace réseau). Mettre des images en BLOB
 * dans une base de données financière est une mauvaise pratique :
 * ça alourdit les sauvegardes et ralentit les requêtes sans raison.
 */
@Entity
@Table(name = "photo_reception")
@Getter
@Setter
public class PhotoReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lien vers la facture concernée. C'est CETTE colonne (facture_id)
     * qui porte réellement la relation en base de données — Facture.java
     * ne fait que "lire" cette relation dans l'autre sens via mappedBy.
     *
     * nullable = false : une photo doit TOUJOURS être liée à une facture,
     * on ne peut pas avoir une photo orpheline.
     */
    @ManyToOne
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    /**
     * Chemin relatif ou absolu du fichier sur le disque, PAS l'image
     * elle-même. Exemple : "/var/prefacture/photos/42/a1b2c3.jpg".
     * Le nom de fichier utilise un UUID (généré dans PhotoService, à
     * venir) pour éviter que deux photos aient accidentellement le
     * même nom et s'écrasent l'une l'autre.
     */
    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    /** Horodatage automatique au moment de la prise de la photo. */
    @Column(name = "date_prise", nullable = false)
    private LocalDateTime datePrise = LocalDateTime.now();

    /**
     * Username de la personne qui a pris la photo — trace d'audit,
     * utile si jamais on doit vérifier qui a déposé quoi.
     */
    @Column(name = "pris_par", nullable = false)
    private String prisPar;
}