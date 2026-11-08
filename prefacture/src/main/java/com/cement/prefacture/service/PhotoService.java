package com.cement.prefacture.service;

import com.cement.prefacture.entity.Facture;
import com.cement.prefacture.entity.PhotoReception;
import com.cement.prefacture.exception.FichierInvalideException;
import com.cement.prefacture.repository.PhotoReceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gère le stockage des photos preuve de réception (voir la discussion
 * initiale : "stocke l'image sur disque, garde seulement le chemin en
 * base"). Cette classe est la SEULE partie du code qui touche
 * directement le système de fichiers pour les photos — ni le
 * contrôleur, ni les autres services n'écrivent de fichiers eux-mêmes.
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoReceptionRepository photoReceptionRepository;
    private final FactureService factureService;

    // Injecté depuis application.properties → app.upload-dir
    // (./data/photos en dev, /var/prefacture/photos en prod)
    @Value("${app.upload-dir}")
    private String dossierUpload;

    // Types de fichiers autorisés — refuse tout le reste (pdf, texte,
    // exécutables déguisés en image, etc.)
    private static final Set<String> TYPES_AUTORISES = Set.of("image/jpeg", "image/png", "image/webp");

    // Limite en octets — cohérent avec spring.servlet.multipart.max-file-size (10MB)
    private static final long TAILLE_MAX_OCTETS = 10 * 1024 * 1024;

    /**
     * Valide, stocke sur disque, puis enregistre une nouvelle photo
     * de réception liée à une facture.
     */
    @Transactional
    public PhotoReception enregistrerPhoto(Long factureId, MultipartFile fichier, String username) {
        validerFichier(fichier);

        Facture facture = factureService.obtenirParId(factureId);
        String cheminComplet = sauvegarderSurDisque(factureId, fichier);

        PhotoReception photo = new PhotoReception();
        photo.setFacture(facture);
        photo.setCheminFichier(cheminComplet);
        photo.setDatePrise(LocalDateTime.now());
        photo.setPrisPar(username);

        return photoReceptionRepository.save(photo);
    }

    /**
     * Vérifie que le fichier reçu est bien une image, d'un type
     * autorisé, et pas vide. On vérifie AVANT d'écrire quoi que ce
     * soit sur le disque — pas la peine de créer des fichiers qu'on
     * va devoir supprimer juste après en cas de problème.
     */
    private void validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new FichierInvalideException("Aucun fichier reçu.");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new FichierInvalideException("Fichier trop volumineux (max 10MB).");
        }
        if (!TYPES_AUTORISES.contains(fichier.getContentType())) {
            throw new FichierInvalideException(
                    "Type de fichier non autorisé : " + fichier.getContentType() +
                            ". Formats acceptés : JPEG, PNG, WEBP."
            );
        }
    }

    /**
     * Écrit le fichier sur le disque, dans un sous-dossier par facture
     * (ex: ./data/photos/42/a1b2c3d4.jpg), avec un nom de fichier
     * généré par UUID pour éviter toute collision entre deux photos
     * prises au même moment par deux utilisateurs différents.
     */
    private String sauvegarderSurDisque(Long factureId, MultipartFile fichier) {
        try {
            Path dossierFacture = Paths.get(dossierUpload, String.valueOf(factureId));
            Files.createDirectories(dossierFacture);

            String extension = extraireExtension(fichier.getOriginalFilename());
            String nomFichier = UUID.randomUUID() + extension;
            Path cheminFinal = dossierFacture.resolve(nomFichier);

            fichier.transferTo(cheminFinal);

            return cheminFinal.toString();
        } catch (IOException ex) {
            throw new FichierInvalideException("Impossible d'enregistrer le fichier : " + ex.getMessage());
        }
    }

    private String extraireExtension(String nomOriginal) {
        if (nomOriginal == null || !nomOriginal.contains(".")) {
            return ".jpg"; // valeur par défaut si l'extension est absente
        }
        return nomOriginal.substring(nomOriginal.lastIndexOf('.'));
    }

    /** Renvoie toutes les photos liées à une facture (écran de détail). */
    public List<PhotoReception> listerParFacture(Long factureId) {
        return photoReceptionRepository.findByFactureId(factureId);
    }
}