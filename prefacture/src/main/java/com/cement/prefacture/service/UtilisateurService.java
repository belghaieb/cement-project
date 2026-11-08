package com.cement.prefacture.service;

import com.cement.prefacture.dto.CreerUtilisateurRequest;
import com.cement.prefacture.dto.UtilisateurReponse;
import com.cement.prefacture.entity.Utilisateur;
import com.cement.prefacture.enums.Role;
import com.cement.prefacture.exception.RegleMetierException;
import com.cement.prefacture.exception.RessourceNonTrouveeException;
import com.cement.prefacture.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique métier de gestion des comptes — RÉSERVÉ à l'Admin (voir
 * SecurityConfig : /admin/** exige hasRole("ADMIN")).
 *
 * C'est la seule façon de créer un compte Responsable Facture ou IT :
 * il n'y a pas d'écran d'inscription publique, volontairement — un
 * outil interne d'entreprise ne doit pas permettre à n'importe qui
 * de créer son propre compte.
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UtilisateurReponse creer(CreerUtilisateurRequest requete) {
        if (utilisateurRepository.findByUsername(requete.getUsername()).isPresent()) {
            throw new RegleMetierException("Ce nom d'utilisateur existe déjà : " + requete.getUsername());
        }

        Role role;
        try {
            role = Role.valueOf(requete.getRole());
        } catch (IllegalArgumentException ex) {
            throw new RegleMetierException("Rôle invalide : " + requete.getRole() + " (attendu : ADMIN, RESPONSABLE_FACTURE ou IT)");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(requete.getUsername());
        utilisateur.setPassword(passwordEncoder.encode(requete.getPassword()));
        utilisateur.setNomComplet(requete.getNomComplet());
        utilisateur.setRole(role);
        utilisateur.setActif(true);

        Utilisateur sauvegarde = utilisateurRepository.save(utilisateur);
        return versReponse(sauvegarde);
    }

    public List<UtilisateurReponse> listerTous() {
        return utilisateurRepository.findAll().stream()
                .map(this::versReponse)
                .toList();
    }

    /**
     * Désactive un compte (voir Utilisateur.actif) — préférable à une
     * suppression : garde l'historique (qui a validé quelles factures)
     * intact même après le départ d'un employé.
     */
    @Transactional
    public UtilisateurReponse desactiver(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur introuvable : " + id));
        utilisateur.setActif(false);
        return versReponse(utilisateurRepository.save(utilisateur));
    }

    private UtilisateurReponse versReponse(Utilisateur u) {
        return new UtilisateurReponse(u.getId(), u.getUsername(), u.getNomComplet(), u.getRole().name(), u.isActif());
    }
}