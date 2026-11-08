package com.cement.prefacture.ocr;

import com.cement.prefacture.dto.ChampExtrait;
import com.cement.prefacture.dto.ResultatOcr;
import com.cement.prefacture.exception.OcrEchecException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final GeminiOcrService geminiOcrService;

    @Value("${ocr.tesseract.data-path}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language}")
    private String tesseractLanguage;


    // 5s était bien trop court : un appel Groq (vision + modèle
    // "raisonneur" qui génère un bloc <think> avant sa réponse) prend
    // facilement 10-20+ secondes selon la taille de l'image. Avec 5s,
    // le thread était systématiquement interrompu en pleine requête
    // HTTP, ce qui levait une InterruptedException — dont le message
    // est TOUJOURS null, d'où le "Gemini indisponible : null" trompeur
    // qui n'avait rien à voir avec une vraie indisponibilité de l'API.
    private static final int TIMEOUT_SECONDS = 45;


    private static final Pattern PATTERN_NUMERO_FACTURE =
            Pattern.compile("FA\\s?\\d{5,8}");

    private static final Pattern PATTERN_DATE =
            Pattern.compile("\\d{2}/\\d{2}/\\d{4}");

    private static final Pattern PATTERN_BC =
            Pattern.compile("BC[A-Z0-9]{6,}");


    private ITesseract creerTesseract() {

        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage(tesseractLanguage);

        return tesseract;
    }



    public ResultatOcr analyser(File fichierImage) {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();


        Future<ResultatOcr> future =
                executor.submit(() -> analyserInterne(fichierImage));


        try {

            return future.get(
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );


        } catch (TimeoutException e) {

            future.cancel(true);

            log.warn("OCR annulé après {}s (timeout) sur {}", TIMEOUT_SECONDS, fichierImage.getName());

            return resultatVide(
                    "OCR trop lent, veuillez compléter manuellement"
            );


        } catch (Exception e) {

            throw new OcrEchecException(
                    "Impossible d'analyser l'image reçue",
                    e
            );

        } finally {

            executor.shutdownNow();
        }
    }





    private ResultatOcr analyserInterne(File fichierImage)
            throws IOException, TesseractException {


        BufferedImage image =
                ImageIO.read(fichierImage);


        if(image == null){

            throw new IOException(
                    "Format image non supporté : "
                            + fichierImage.getName()
            );
        }



        ITesseract tesseract =
                creerTesseract();



        String texte =
                tesseract.doOCR(image);



        List<Word> mots =
                tesseract.getWords(
                        image,
                        ITessAPI.TessPageIteratorLevel.RIL_WORD
                );


        int confiance =
                calculerConfiance(mots);



        List<ChampExtrait> champs =
                extraireDepuisTexte(
                        texte,
                        confiance
                );



        try {


            List<ChampExtrait> gemini =
                    geminiOcrService
                            .extraireChamps(fichierImage);



            fusionner(
                    champs,
                    gemini
            );



        } catch(Exception e){

            // Auparavant : System.out.println(... + e.getMessage())
            // Deux problèmes avec ça : (1) System.out n'apparaît pas
            // proprement dans les logs structurés/horodatés, et (2)
            // e.getMessage() peut être null (ex: InterruptedException),
            // ce qui masquait complètement la vraie cause. En passant
            // l'exception en dernier argument au logger SLF4J, la trace
            // complète est affichée — indispensable pour diagnostiquer
            // les vrais échecs Groq (mauvais modèle, quota, JSON
            // invalide...) au lieu de deviner à l'aveugle.
            log.warn("Gemini/Groq indisponible, on continue avec Tesseract seul", e);

        }




        ResultatOcr resultat =
                new ResultatOcr();


        resultat.setTexteBrut(texte);

        resultat.setChamps(champs);

        resultat.setConfianceMoyenne(confiance);


        resultat.setNecessiteVerification(
                champs.isEmpty()
        );


        return resultat;

    }






    private List<ChampExtrait> extraireDepuisTexte(
            String texte,
            int confiance
    ){

        List<ChampExtrait> result =
                new ArrayList<>();



        Matcher numero =
                PATTERN_NUMERO_FACTURE.matcher(texte);


        if(numero.find()){

            result.add(
                    new ChampExtrait(
                            "numeroFacture",
                            numero.group(),
                            confiance
                    )
            );

        }



        Matcher date =
                PATTERN_DATE.matcher(texte);


        if(date.find()){

            result.add(
                    new ChampExtrait(
                            "dateFacture",
                            date.group(),
                            confiance
                    )
            );

        }



        Matcher bc =
                PATTERN_BC.matcher(texte);


        if(bc.find()){

            result.add(
                    new ChampExtrait(
                            "bonDeCommande",
                            bc.group(),
                            confiance
                    )
            );

        }


        return result;

    }







    private void fusionner(
            List<ChampExtrait> existants,
            List<ChampExtrait> nouveaux
    ){

        for(ChampExtrait nouveau : nouveaux){

            existants.removeIf(
                    e -> e.getNom()
                            .equals(nouveau.getNom())
            );


            existants.add(nouveau);
        }

    }







    private int calculerConfiance(
            List<Word> mots
    ){

        if(mots==null || mots.isEmpty())
            return 0;


        double total=0;


        for(Word mot:mots){

            total += mot.getConfidence();

        }


        return (int)
                Math.round(
                        total / mots.size()
                );
    }







    private ResultatOcr resultatVide(
            String message
    ){

        ResultatOcr resultat =
                new ResultatOcr();


        resultat.setTexteBrut(message);

        resultat.setChamps(
                new ArrayList<>()
        );


        resultat.setConfianceMoyenne(0);


        resultat.setNecessiteVerification(true);


        return resultat;

    }

}