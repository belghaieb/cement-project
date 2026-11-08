package com.cement.prefacture.config;

import com.cement.prefacture.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Fait le lien entre Spring Security et notre base de données.
 *
 * Spring Security ne sait pas, par défaut, où chercher les utilisateurs.
 * En implémentant UserDetailsService, on lui dit exactement comment
 * charger un utilisateur : "va voir dans UtilisateurRepository".
 *
 * Cette classe est appelée AUTOMATIQUEMENT par Spring Security à chaque
 * tentative de connexion (via AuthenticationManager) — on ne l'appelle
 * jamais nous-mêmes directement dans nos contrôleurs.
 */
@Service
@RequiredArgsConstructor
// @RequiredArgsConstructor (Lombok) génère un constructeur qui prend
// tous les champs "final" en paramètre — ici juste utilisateurRepository.
// C'est ce qu'on appelle l'injection de dépendances par constructeur :
// Spring fournit automatiquement l'implémentation de UtilisateurRepository
// quand il crée cette classe, sans qu'on ait besoin d'écrire "new ...".
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Méthode appelée par Spring Security pendant l'authentification.
     *
     * Notre entité Utilisateur implémente déjà UserDetails (voir
     * Utilisateur.java), donc on peut la renvoyer directement — pas
     * besoin de la convertir vers une autre classe.
     *
     * Si l'utilisateur n'existe pas, on lève UsernameNotFoundException :
     * Spring Security capture cette exception lui-même et la transforme
     * en échec d'authentification (identifiants invalides), sans jamais
     * révéler si c'est le username ou le mot de passe qui est faux —
     * c'est volontaire, pour ne pas donner d'indice à un attaquant.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));
    }
}