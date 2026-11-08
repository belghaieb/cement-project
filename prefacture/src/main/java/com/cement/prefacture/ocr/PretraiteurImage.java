package com.cement.prefacture.ocr;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * Prépare une version "améliorée" d'une image avant un second passage
 * OCR, utilisée uniquement quand le premier passage a une confiance
 * trop basse (voir OcrService.SEUIL_CONFIANCE).
 *
 * Pourquoi c'est nécessaire : relancer Tesseract sur EXACTEMENT la même
 * image donnera EXACTEMENT le même résultat (Tesseract n'est pas
 * aléatoire) — comparer deux passages identiques ne prouve rien. Pour
 * qu'une comparaison ait un sens, il faut donner à Tesseract une image
 * réellement différente à analyser.
 */
@Component // Permet à Spring de créer et injecter cette classe automatiquement
public class PretraiteurImage {

    /**
     * Convertit l'image en noir et blanc pur (binarisation), après
     * l'avoir d'abord passée en niveaux de gris.
     *
     * Étape 1 — Niveaux de gris : formule standard de luminance
     * (0.299*R + 0.587*G + 0.114*B), qui donne plus de poids au vert
     * car l'œil humain (et les capteurs de caméra) y sont plus
     * sensibles.
     *
     * Étape 2 — Binarisation : chaque pixel devient soit noir (0) soit
     * blanc (255) selon un seuil fixe (150). Ça élimine le bruit, les
     * ombres légères et les variations de luminosité d'une photo prise
     * au téléphone — souvent ce qui perturbe le premier passage OCR.
     */
    public BufferedImage ameliorerContraste(BufferedImage original) {
        int largeur = original.getWidth();
        int hauteur = original.getHeight();
        BufferedImage resultat = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int rgb = original.getRGB(x, y);

                // Extraction des composantes rouge/vert/bleu du pixel
                // via des décalages de bits (le format RGB encode les
                // 3 couleurs dans un seul entier de 32 bits).
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gris = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // Seuil à 150 : en dessous → noir, au-dessus → blanc.
                // Cette valeur peut être ajustée si les scans réels
                // sont trop clairs ou trop sombres une fois testés.
                int valeur = gris < 150 ? 0 : 255;
                int pixelGris = (valeur << 16) | (valeur << 8) | valeur;
                resultat.setRGB(x, y, pixelGris);
            }
        }
        return resultat;
    }
}