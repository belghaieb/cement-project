package com.cement.prefacture.controller;

import com.cement.prefacture.dto.FactureAvecAlertes;
import com.cement.prefacture.dto.ResultatOcr;
import com.cement.prefacture.entity.Facture;
import com.cement.prefacture.ocr.OcrService;
import com.cement.prefacture.service.FactureService;
import com.cement.prefacture.enums.StatutFacture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Expose les factures via HTTP — c'est ce que factures.html et
 * facture.html appellent réellement en coulisses (une fois qu'on aura
 * remplacé les données statiques de démo par de vrais appels fetch()).
 *
 * RAPPEL IMPORTANT : ce contrôleur ne contient AUCUNE logique métier.
 * Il ne fait que : recevoir la requête HTTP → appeler le bon service →
 * renvoyer le résultat. Toutes les vraies règles (validation, statut,
 * etc.) vivent dans FactureService — voir ce fichier pour comprendre
 * le "pourquoi" des décisions.
 */
@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;
    private final OcrService ocrService;

    /**
     * Liste toutes les factures — utilisé par factures.html pour
     * remplir le tableau (actuellement rempli avec des données de
     * démonstration codées en dur dans le <script> de la page).
     *
     * Accessible aux 3 rôles (voir SecurityConfig : GET /factures/**
     * est ouvert à ADMIN, RESPONSABLE_FACTURE et IT).
     */
    @GetMapping
    public ResponseEntity<List<Facture>> listerToutes(
            @RequestParam(required = false) StatutFacture statut,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin) {
        return ResponseEntity.ok(factureService.listerAvecFiltres(statut, dateDebut, dateFin));
    }

    /**
     * Action réservée à l'IT : confirme que la marchandise a bien
     * été reçue physiquement (indépendant de valider/rejeter, qui
     * reste réservé au Responsable Facture).
     */
    @PostMapping("/{id}/confirmer-reception")
    public ResponseEntity<Facture> confirmerReception(@PathVariable Long id, Authentication authentification) {
        String username = ((UserDetails) authentification.getPrincipal()).getUsername();
        return ResponseEntity.ok(factureService.confirmerReception(id, username));
    }

    /**
     * Détail d'une facture précise — utilisé par facture.html.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Facture> obtenirParId(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.obtenirParId(id));
    }

    /**
     * Reçoit une image scannée, lance l'OCR (Tesseract + Gemini si
     * besoin, voir OcrService), et crée automatiquement une nouvelle
     * Facture à partir des champs extraits.
     *
     * C'est le point d'entrée du flux "photo → facture" dont on a
     * discuté : le frontend envoie le fichier ici juste après la
     * prise de photo (input type="file" capture="environment").
     *
     * Réservé à ADMIN et RESPONSABLE_FACTURE (voir SecurityConfig,
     * POST /factures/** exclut IT).
     */
    @PostMapping("/scan")
    public ResponseEntity<FactureAvecAlertes> scannerNouvelleFacture(
            @RequestParam("image") MultipartFile image,
            @org.springframework.beans.factory.annotation.Value("${app.upload-dir}") String dossierUpload) throws IOException {

        String extension = extraireExtension(image.getContentType(), image.getOriginalFilename());

        // On sauvegarde le scan de façon PERMANENTE cette fois (contrairement
        // à avant où le fichier était supprimé juste après l'OCR) — l'écran
        // de révision (facture.html) a besoin de pouvoir réafficher l'image
        // originale à côté des champs extraits, pas seulement le texte lu.
        java.nio.file.Path dossierScans = java.nio.file.Paths.get(dossierUpload, "scans");
        Files.createDirectories(dossierScans);
        String nomFichier = UUID.randomUUID() + extension;
        java.nio.file.Path cheminFinal = dossierScans.resolve(nomFichier);
        image.transferTo(cheminFinal);

        ResultatOcr resultat = ocrService.analyser(cheminFinal.toFile());
        FactureAvecAlertes resultatFinal = factureService.creerDepuisOcr(resultat, cheminFinal.toString());
        return ResponseEntity.ok(resultatFinal);
    }

    /**
     * Renvoie l'IMAGE SCAN ORIGINALE d'une facture — celle envoyée au
     * moment du scan, pas une photo de réception (voir PhotoController
     * pour ça, qui est un concept différent : preuve de livraison
     * physique). Accessible en lecture aux 3 rôles, comme le reste des
     * factures (GET /factures/** est ouvert à IT aussi).
     */
    @GetMapping("/{id}/image-scan")
    public ResponseEntity<org.springframework.core.io.Resource> obtenirImageScan(@PathVariable Long id) {
        Facture facture = factureService.obtenirParId(id);

        if (facture.getCheminImageScan() == null) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource ressource =
                new org.springframework.core.io.FileSystemResource(new File(facture.getCheminImageScan()));

        String contentType = facture.getCheminImageScan().toLowerCase().endsWith(".png")
                ? "image/png" : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(ressource);
    }

    private String extraireExtension(String contentType, String nomOriginal) {
        if (contentType != null) {
            if (contentType.equals("image/png")) return ".png";
            if (contentType.equals("image/jpeg")) return ".jpg";
            if (contentType.equals("image/webp")) return ".webp";
        }
        if (nomOriginal != null && nomOriginal.contains(".")) {
            return nomOriginal.substring(nomOriginal.lastIndexOf('.'));
        }
        return ".jpg"; // dernier recours
    }

    /**
     * Met à jour les champs d'une facture (corrections manuelles
     * après révision à l'écran facture.html, AVANT validation finale).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Facture> mettreAJour(@PathVariable Long id, @RequestBody Facture donneesModifiees) {
        return ResponseEntity.ok(factureService.mettreAJour(id, donneesModifiees));
    }

    /**
     * Valide une facture — correspond au bouton "Valider la facture"
     * déjà présent dans facture.html (id="btn-validate").
     *
     * "Authentication authentification" est injecté automatiquement
     * par Spring Security : il représente l'utilisateur actuellement
     * connecté (celui qui a le droit d'appeler cette route, déjà
     * vérifié par SecurityConfig avant même d'arriver ici). On
     * récupère son username pour tracer QUI a validé la facture.
     */
    @PostMapping("/{id}/valider")
    public ResponseEntity<Facture> valider(@PathVariable Long id, Authentication authentification) {
        String username = ((UserDetails) authentification.getPrincipal()).getUsername();
        return ResponseEntity.ok(factureService.valider(id, username));
    }

    /**
     * Rejette une facture — correspond au bouton "Rejeter"
     * (id="btn-reject") déjà présent dans facture.html.
     */
    @PostMapping("/{id}/rejeter")
    public ResponseEntity<Facture> rejeter(@PathVariable Long id, Authentication authentification) {
        String username = ((UserDetails) authentification.getPrincipal()).getUsername();
        return ResponseEntity.ok(factureService.rejeter(id, username));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {

        factureService.supprimer(id);

        return ResponseEntity.noContent().build();
    }
}