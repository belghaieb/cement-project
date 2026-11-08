package com.cement.prefacture.repository;

import com.cement.prefacture.entity.PhotoReception;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Interface d'accès aux données pour PhotoReception.
 */
public interface PhotoReceptionRepository extends JpaRepository<PhotoReception, Long> {

    /**
     * Renvoie toutes les photos liées à une facture donnée.
     *
     * Ici le nom est "findByFactureId" et non "findByFacture" — Spring
     * comprend qu'on veut filtrer sur l'id de la relation @ManyToOne
     * "facture" définie dans PhotoReception.java, sans avoir besoin de
     * charger l'objet Facture complet juste pour filtrer.
     *
     * Utile pour l'écran de détail d'une facture (facture.html) :
     * on affiche toutes les photos prises pour cette facture précise,
     * même s'il y en a plusieurs (recto/verso, ou une reprise après
     * une photo floue).
     */
    List<PhotoReception> findByFactureId(Long factureId);
}