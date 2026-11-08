package com.cement.prefacture.repository;

import com.cement.prefacture.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Interface d'accès aux données pour Utilisateur.
 *
 * "extends JpaRepository<Utilisateur, Long>" veut dire :
 *   - Utilisateur  : le type d'entité géré
 *   - Long          : le type de la clé primaire (id)
 *
 * Juste avec cette ligne, Spring Data JPA génère AUTOMATIQUEMENT,
 * sans qu'on écrive une seule ligne de code d'implémentation :
 *   - save(Utilisateur u)          → insère ou met à jour
 *   - findById(Long id)            → cherche par id
 *   - findAll()                    → renvoie tous les utilisateurs
 *   - deleteById(Long id)          → supprime par id
 * ...et une dizaine d'autres méthodes standard.
 *
 * On n'a besoin d'écrire QUE les méthodes personnalisées, comme
 * celle ci-dessous — Spring déduit la requête directement du nom
 * de la méthode.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /**
     * Cherche un utilisateur par son identifiant de connexion.
     *
     * Spring lit le nom de cette méthode ("findBy" + "Username") et
     * génère automatiquement la requête équivalente à :
     *   SELECT * FROM utilisateur WHERE username = ?
     *
     * Optional<Utilisateur> plutôt que Utilisateur directement : ça
     * force le code appelant (UtilisateurDetailsService) à gérer
     * explicitement le cas où l'utilisateur n'existe pas, au lieu de
     * risquer un NullPointerException plus loin dans le code.
     */
    Optional<Utilisateur> findByUsername(String username);
}