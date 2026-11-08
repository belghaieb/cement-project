package com.cement.prefacture.entity;

import com.cement.prefacture.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Représente un utilisateur interne de l'application Préfacture.
 *
 * Cette classe a un double rôle :
 *   1. C'est une entité JPA : Hibernate la transforme en table "utilisateur"
 *      dans la base de données (une ligne = un utilisateur).
 *   2. Elle implémente UserDetails (interface de Spring Security), ce qui
 *      permet à Spring Security de l'utiliser DIRECTEMENT pour authentifier
 *      quelqu'un — pas besoin d'une classe séparée juste pour la sécurité.
 */
@Entity
@Table(name = "utilisateur")
@Getter // Lombok génère automatiquement tous les getters (getId(), getUsername(), etc.)
@Setter // Lombok génère automatiquement tous les setters (setId(), setUsername(), etc.)
public class Utilisateur implements UserDetails {

    /**
     * Identifiant unique en base, généré automatiquement par MySQL/H2
     * (auto-increment). On ne le définit jamais nous-mêmes.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant de connexion (ex: "a.belghaieb").
     * "unique = true" empêche la création de deux comptes avec le même
     * username — la base de données rejettera la deuxième insertion.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Mot de passe — TOUJOURS stocké chiffré (BCrypt), jamais en clair.
     * Le chiffrement se fait via PasswordEncoder dans SecurityConfig,
     * pas ici. Cette classe ne fait que stocker la valeur déjà chiffrée.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Nom complet affiché dans l'interface (ex: "Aziz Belghaieb"),
     * différent du username technique.
     */
    @Column(nullable = false)
    private String nomComplet;

    /**
     * Rôle de l'utilisateur : ADMIN, RESPONSABLE_FACTURE ou IT.
     * @Enumerated(EnumType.STRING) stocke le nom du rôle en texte dans la
     * base ("ADMIN") plutôt qu'un numéro — plus lisible si on consulte la
     * table directement, et plus sûr si on réordonne l'enum plus tard.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Permet de désactiver un compte sans le supprimer (ex: employé qui
     * quitte l'entreprise). Un compte inactif ne peut plus se connecter.
     */
    @Column(nullable = false)
    private boolean actif = true;

    // ============================================================
    // Méthodes exigées par l'interface UserDetails (Spring Security)
    // Spring Security appelle CES méthodes automatiquement à chaque
    // tentative de connexion — on ne les appelle jamais nous-mêmes.
    // ============================================================

    /**
     * Renvoie les "autorités" (permissions) de cet utilisateur.
     * Spring Security utilise le préfixe "ROLE_" par convention —
     * c'est ce préfixe que SecurityConfig attend (hasRole("ADMIN")
     * cherche en fait "ROLE_ADMIN" en interne).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Le compte n'expire jamais automatiquement dans notre cas. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Pas de mécanisme de verrouillage automatique pour l'instant. */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** Le mot de passe n'expire jamais automatiquement dans notre cas. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Spring Security appelle cette méthode pour vérifier si le compte
     * est activé. On relie directement au champ "actif" : un compte
     * désactivé ne pourra plus se connecter, sans avoir à le supprimer.
     */
    @Override
    public boolean isEnabled() {
        return actif;
    }
}