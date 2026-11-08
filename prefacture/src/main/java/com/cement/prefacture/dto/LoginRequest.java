package com.cement.prefacture.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Représente le corps JSON envoyé par le frontend lors d'une tentative
 * de connexion : { "username": "...", "password": "..." }
 *
 * C'est un DTO (Data Transfer Object) — une classe qui ne fait QUE
 * transporter des données entre le frontend et le backend, sans
 * logique métier et sans lien avec la base de données (contrairement
 * à Utilisateur.java, qui EST liée à la base via @Entity).
 *
 * On ne réutilise jamais l'entité Utilisateur directement pour recevoir
 * une requête de login : elle contient des champs qu'on ne veut jamais
 * exposer/accepter depuis l'extérieur (comme "role" ou "actif") — un DTO
 * limite précisément ce que le client peut envoyer.
 */
@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
}