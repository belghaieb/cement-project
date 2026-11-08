package com.cement.prefacture.config;

import com.cement.prefacture.entity.Utilisateur;
import com.cement.prefacture.enums.Role;
import com.cement.prefacture.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée automatiquement un compte Admin au premier démarrage, UNIQUEMENT
 * si la table utilisateur est vide — sans ça, impossible de se
 * connecter la toute première fois (aucun compte n'existe encore, et
 * il n'y a pas d'écran d'inscription).
 *
 * CommandLineRunner : Spring exécute automatiquement la méthode run()
 * une fois, juste après le démarrage complet de l'application.
 *
 * ⚠️ Change ce mot de passe immédiatement après le premier login —
 * il est ici en clair uniquement pour l'amorçage initial.
 */
@Component
@RequiredArgsConstructor
public class InitialisationDonnees implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (utilisateurRepository.count() == 0) {
            creerUtilisateur("admin", "admin123", "Administrateur SCG", Role.ADMIN);
            creerUtilisateur("responsable", "resp123", "Responsable Facture SCG", Role.RESPONSABLE_FACTURE);
            creerUtilisateur("it", "it123", "IT Consultation SCG", Role.IT);

            System.out.println("========================================");
            System.out.println("3 comptes de test créés automatiquement :");
            System.out.println("   ADMIN               → admin / admin123");
            System.out.println("   RESPONSABLE_FACTURE  → responsable / resp123");
            System.out.println("   IT                   → it / it123");
            System.out.println("   → À changer avant la mise en production");
            System.out.println("========================================");
        }
    }

    private void creerUtilisateur(String username, String motDePasse, String nomComplet, Role role) {
        Utilisateur u = new Utilisateur();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(motDePasse));
        u.setNomComplet(nomComplet);
        u.setRole(role);
        u.setActif(true);
        utilisateurRepository.save(u);
    }
}