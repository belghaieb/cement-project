package com.cement.prefacture.ocr;

import com.cement.prefacture.dto.ChampExtrait;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Second avis OCR utilisant Groq Vision (modèle qwen/qwen3.6-27b),
 * appelé UNIQUEMENT par OcrService quand la confiance Tesseract est
 * trop faible.
 *
 * Anciennement basé sur Gemini Vision (Google) — migré vers Groq suite
 * au retrait du modèle gemini-2.5-flash. Groq expose une API compatible
 * OpenAI (endpoint /chat/completions), donc la structure de requête/
 * réponse est différente de l'ancienne implémentation Gemini.
 *
 * IMPORTANT : qwen/qwen3.6-27b est un modèle "raisonneur" — il renvoie
 * systématiquement un bloc <think>...</think> AVANT la réponse finale,
 * même quand on lui demande explicitement "que du JSON". Le parsing
 * doit donc retirer ce bloc avant de tenter de parser le JSON utile
 * (voir parserReponseJson ci-dessous).
 *
 * NOTE SUR HTTP/1.1 : constaté en test que le client HTTP Java, avec
 * ses réglages par défaut (négociation HTTP/2), recevait parfois une
 * page HTML au lieu du JSON attendu de Groq — alors qu'un test curl
 * identique (HTTP/1.1 par défaut) fonctionnait systématiquement. Cause
 * probable : un antivirus/proxy local qui inspecte le trafic HTTPS et
 * gère mal la négociation HTTP/2. On force donc HTTP/1.1 explicitement
 * ci-dessous pour contourner ce problème.
 *
 * On utilise toujours le client HTTP natif de Java (java.net.http.HttpClient) :
 * pas besoin de SDK externe, un simple appel REST suffit.
 */
@Service
public class GeminiOcrService {

    private static final Logger log = LoggerFactory.getLogger(GeminiOcrService.class);

    @Value("${groq.api.key}")
    private String cleApi;

    @Value("${groq.api.url}")
    private String urlGroq;

    @Value("${groq.api.model}")
    private String modele;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Envoie l'image à Groq avec une instruction précise, et parse
     * sa réponse pour en extraire les mêmes champs que ceux attendus
     * de Tesseract (numeroFacture, dateFacture, montantHT...).
     */
    public List<ChampExtrait> extraireChamps(File fichierImage) throws IOException, InterruptedException {
        String imageBase64 = encoderImageEnBase64(fichierImage);
        String typeMime = deduireTypeMime(fichierImage);
        String prompt = construirePrompt();

        String urlDonneesImage = "data:" + typeMime + ";base64," + imageBase64;

        Map<String, Object> messageUtilisateur = Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", urlDonneesImage))
                )
        );

        String corpsRequete = mapper.writeValueAsString(Map.of(
                "model", modele,
                "messages", List.of(messageUtilisateur)
        ));

        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(urlGroq))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cleApi)
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(corpsRequete))
                .build();

        HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());

        if (reponse.statusCode() != 200) {
            throw new IOException("Groq a renvoyé une erreur (code " + reponse.statusCode() + ") : " + reponse.body());
        }

        return parserReponseJson(reponse.body());
    }

    /**
     * Convertit le fichier image en base64 — format attendu par Groq
     * pour transmettre une image "inline" via une data URL, dans le
     * champ image_url.url du message.
     */
    private String encoderImageEnBase64(File fichierImage) throws IOException {
        byte[] octets = Files.readAllBytes(fichierImage.toPath());
        return Base64.getEncoder().encodeToString(octets);
    }

    /**
     * Déduit le type MIME depuis l'extension du fichier — nécessaire
     * pour construire une data URL correcte (data:image/xxx;base64,...).
     * Même logique que le fix appliqué côté Tesseract (bug historique :
     * extension .jpg codée en dur causait des erreurs sur les PNG).
     */
    private String deduireTypeMime(File fichierImage) {
        String nom = fichierImage.getName().toLowerCase();
        if (nom.endsWith(".png")) {
            return "image/png";
        } else if (nom.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /**
     * Instruction envoyée à Groq. On lui demande explicitement de
     * répondre en JSON STRICT, sans texte autour — simplifie le parsing
     * côté Java. NOTE : qwen3.6-27b ajoute quand même un bloc <think>
     * avant sa réponse malgré cette consigne (voir parserReponseJson).
     */
    private String construirePrompt() {
        return """
                Tu analyses une image de facture fournisseur. Le format,
                la mise en page et les libellés varient selon le
                fournisseur — adapte-toi au document précis que tu vois,
                ne suppose jamais un format fixe.

                Extrait UNIQUEMENT les champs suivants et réponds avec
                UNIQUEMENT du JSON valide, sans aucun texte avant ou après,
                sans balises markdown :

                {
                  "numeroFacture": "...",
                  "fournisseur": "...",
                  "mfFournisseur": "...",
                  "dateFacture": "JJ/MM/AAAA",
                  "dateEcheance": "JJ/MM/AAAA",
                  "bonDeCommande": "...",
                  "montantHT": "...",
                  "montantTVA": "...",
                  "montantTTC": "...",
                  "devise": "..."
                }

                Précisions importantes (valables quel que soit le format) :
                - "numeroFacture" peut apparaître sous des libellés variés :
                  "Facture N°", "N°", "Numéro", "Ref", etc. — identifie le
                  numéro qui identifie CETTE facture précisément.
                - "fournisseur" est l'entreprise qui ÉMET la facture
                  (généralement en haut du document, avec logo/en-tête),
                  jamais le client destinataire.
                - "montantHT" est le TOTAL HT de la facture entière
                  (généralement dans un récapitulatif en bas de page),
                  jamais le prix ou montant d'une seule ligne de détail.
                - "montantTTC" est le total après TVA — s'il existe un
                  timbre fiscal séparé, indique le Total TTC avant timbre,
                  pas le "Net à payer" final si celui-ci inclut le timbre.
                - "bonDeCommande" peut être absent — certaines factures
                  n'en ont pas, laisse vide plutôt que d'inventer.
                - Si un champ est illisible ou absent, mets une chaîne
                  vide "" plutôt que de deviner.
                """;
    }

    /**
     * Parse la réponse JSON de Groq pour en extraire nos champs.
     *
     * Structure réelle de la réponse Groq (format OpenAI-compatible) :
     * choices[0].message.content contient NOTRE JSON, sous forme de
     * texte — on parse donc deux niveaux : d'abord l'enveloppe Groq,
     * puis le JSON qu'elle contient.
     *
     * qwen/qwen3.6-27b étant un modèle "raisonneur", ce texte commence
     * systématiquement par un bloc <think>...</think> contenant son
     * raisonnement interne, AVANT le JSON utile — ce bloc doit être
     * retiré en premier, sinon le parsing JSON échoue immédiatement
     * (erreur "Unexpected character '<'").
     *
     * Si le parsing de l'ENVELOPPE elle-même échoue (avant même
     * d'atteindre le bloc <think>), c'est le signe que la réponse
     * brute n'est pas du JSON valide du tout (ex: page HTML renvoyée
     * par un proxy/antivirus au lieu de la vraie réponse Groq) — dans
     * ce cas on logue le corps brut (tronqué) pour pouvoir diagnostiquer
     * immédiatement au lieu de deviner à l'aveugle sur un message
     * d'erreur Jackson générique.
     */
    private List<ChampExtrait> parserReponseJson(String corpsReponse) throws IOException {
        JsonNode racine;
        try {
            racine = mapper.readTree(corpsReponse);
        } catch (Exception ex) {
            String extrait = corpsReponse.length() > 500 ? corpsReponse.substring(0, 500) + "..." : corpsReponse;
            log.warn("Réponse Groq non-JSON reçue (extrait) : {}", extrait);
            throw new IOException("Réponse Groq illisible (pas du JSON) : " + extrait, ex);
        }

        String texteJson = racine
                .path("choices").get(0)
                .path("message")
                .path("content")
                .asText();

        // 1) Retrait du bloc de raisonnement <think>...</think> — le
        //    flag (?s) fait que "." capture aussi les retours à la
        //    ligne, car ce bloc s'étend sur plusieurs lignes.
        // 2) Retrait d'éventuelles balises markdown (```json) par
        //    sécurité, au cas où le modèle en ajoute malgré la consigne.
        String jsonNettoye = texteJson
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        if (jsonNettoye.isBlank()) {
            throw new IOException("Réponse Groq vide après nettoyage (contenu brut : " + texteJson + ")");
        }

        JsonNode champsNode = mapper.readTree(jsonNettoye);

        List<ChampExtrait> champs = new ArrayList<>();
        // Confiance fixée à 90 par convention pour les champs Groq :
        // pas de score de confiance par mot fourni par le modèle — le
        // vrai signal de fiabilité vient de la COMPARAISON avec
        // Tesseract dans OcrService, pas de ce chiffre isolé.
        ajouterSiPresent(champs, champsNode, "numeroFacture");
        ajouterSiPresent(champs, champsNode, "fournisseur");
        ajouterSiPresent(champs, champsNode, "mfFournisseur");
        ajouterSiPresent(champs, champsNode, "dateFacture");
        ajouterSiPresent(champs, champsNode, "dateEcheance");
        ajouterSiPresent(champs, champsNode, "bonDeCommande");
        ajouterSiPresent(champs, champsNode, "montantHT");
        ajouterSiPresent(champs, champsNode, "montantTVA");
        ajouterSiPresent(champs, champsNode, "montantTTC");
        ajouterSiPresent(champs, champsNode, "devise");

        return champs;
    }

    private void ajouterSiPresent(List<ChampExtrait> champs, JsonNode noeud, String nomChamp) {
        String valeur = noeud.path(nomChamp).asText("");
        if (!valeur.isBlank()) {
            champs.add(new ChampExtrait(nomChamp, valeur, 90));
        }
    }
}