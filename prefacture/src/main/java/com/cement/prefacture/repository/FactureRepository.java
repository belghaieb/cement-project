package com.cement.prefacture.repository;

import com.cement.prefacture.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Interface d'accès aux données pour Facture.
 *
 * Comme UtilisateurRepository, on obtient gratuitement save(),
 * findById(), findAll(), delete()... juste en héritant de
 * JpaRepository<Facture, Long>.
 */
public interface FactureRepository extends JpaRepository<Facture, Long> {

    /**
     * Cherche une facture par son numéro unique (ex: "FA-2026-01846").
     *
     * C'est LA méthode centrale du futur ImportService : avant
     * d'insérer une ligne venue du fichier Sage, on appelle cette
     * méthode pour savoir si la facture existe déjà :
     *
     *   - Optional vide  → nouvelle facture, on l'insère
     *   - Optional rempli → facture déjà connue, on décide si on la
     *                       met à jour ou si on l'ignore, selon son
     *                       statut actuel (voir la logique métier
     *                       discutée pour ImportService)
     */
    Optional<Facture> findByNumeroFacture(String numeroFacture);
}