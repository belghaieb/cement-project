package com.cement.prefacture.controller;

import com.cement.prefacture.entity.PhotoReception;
import com.cement.prefacture.exception.RessourceNonTrouveeException;
import com.cement.prefacture.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * Gère la capture et la consultation des photos preuve de réception
 * (voir la discussion initiale : "photographier la facture à son
 * arrivée physique à l'usine").
 *
 * Toutes les routes sont imbriquées sous /factures/{factureId}/photos
 * plutôt que d'avoir un /photos séparé : une photo n'existe jamais
 * "toute seule", elle est toujours liée à une facture précise — l'URL
 * reflète directement cette relation.
 */
@RestController
@RequestMapping("/factures/{factureId}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * Upload d'une nouvelle photo depuis le téléphone/navigateur.
     *
     * Côté frontend, ça correspond exactement à :
     *   <input type="file" accept="image/*" capture="environment">
     * envoyé ensuite via un FormData en JavaScript.
     *
     * Réservé à ADMIN et RESPONSABLE_FACTURE (voir SecurityConfig :
     * /factures/*\/photos/** exclut IT — cohérent avec son rôle en
     * lecture seule).
     */
    @PostMapping
    public ResponseEntity<PhotoReception> uploader(
            @PathVariable Long factureId,
            @RequestParam("photo") MultipartFile fichier,
            Authentication authentification) {

        String username = ((UserDetails) authentification.getPrincipal()).getUsername();
        PhotoReception photo = photoService.enregistrerPhoto(factureId, fichier, username);
        return ResponseEntity.ok(photo);
    }

    /**
     * Liste toutes les photos d'une facture — utile si plusieurs
     * photos ont été prises (recto/verso, ou une reprise après un
     * premier essai flou).
     */
    @GetMapping
    public ResponseEntity<List<PhotoReception>> lister(@PathVariable Long factureId) {
        return ResponseEntity.ok(photoService.listerParFacture(factureId));
    }

    /**
     * Renvoie le FICHIER IMAGE lui-même (pas juste ses métadonnées),
     * pour affichage dans le navigateur (ex: <img src="...">).
     *
     * C'est la pièce manquante identifiée dès le début de la
     * discussion sur le stockage : on ne rend jamais le dossier de
     * photos public/statique — cette route passe par SecurityConfig
     * comme n'importe quelle autre requête, donc l'accès reste
     * contrôlé par rôle même si quelqu'un devine une URL de photo.
     */
    @GetMapping("/{photoId}")
    public ResponseEntity<Resource> telechargerPhoto(@PathVariable Long factureId, @PathVariable Long photoId) {
        PhotoReception photo = photoService.listerParFacture(factureId).stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new RessourceNonTrouveeException("Photo introuvable : " + photoId));

        Resource ressource = new FileSystemResource(new File(photo.getCheminFichier()));

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(ressource);
    }
}