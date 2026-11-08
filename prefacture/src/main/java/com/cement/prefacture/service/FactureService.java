package com.cement.prefacture.service;

import com.cement.prefacture.dto.ChampExtrait;
import com.cement.prefacture.dto.FactureAvecAlertes;
import com.cement.prefacture.dto.ResultatOcr;
import com.cement.prefacture.dto.ResultatVerificationBc;
import com.cement.prefacture.entity.Facture;
import com.cement.prefacture.enums.StatutFacture;
import com.cement.prefacture.exception.RegleMetierException;
import com.cement.prefacture.exception.RessourceNonTrouveeException;
import com.cement.prefacture.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureService {

    private final FactureRepository factureRepository;
    private final BonCommandeService bonCommandeService;

    private static final DateTimeFormatter FORMAT_DATE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");


    @Transactional
    public FactureAvecAlertes creerDepuisOcr(ResultatOcr resultat, String cheminImageScan) {

        Facture facture = new Facture();

        facture.setStatut(StatutFacture.A_VERIFIER);
        facture.setConfianceMoyenne(resultat.getConfianceMoyenne());
        facture.setDateReception(LocalDateTime.now());
        facture.setCheminImageScan(cheminImageScan);


        // récupération des données OCR
        for (ChampExtrait champ : resultat.getChamps()) {
            appliquerChamp(facture, champ);
        }


        /*
         * Champs obligatoires.
         * Si OCR ne trouve pas ces informations :
         * on ne crée pas une fausse facture.
         * L'utilisateur complète manuellement.
         */
        if (facture.getNumeroFacture() == null ||
                facture.getNumeroFacture().isBlank()) {

            throw new RegleMetierException(
                    "Numéro de facture introuvable. Veuillez compléter manuellement."
            );
        }


        if (facture.getMontantHT() == null) {

            throw new RegleMetierException(
                    "Montant HT introuvable. Veuillez compléter manuellement."
            );
        }


        /*
         * Protection doublon
         */
        verifierDoublon(facture);



        Facture factureSauvegardee =
                factureRepository.save(facture);



        ResultatVerificationBc verification =
                bonCommandeService.verifier(
                        factureSauvegardee.getBonDeCommande()
                );


        return new FactureAvecAlertes(
                factureSauvegardee,
                verification.getAlertes()
        );
    }



    private void verifierDoublon(Facture facture) {

        factureRepository
                .findByNumeroFacture(facture.getNumeroFacture())
                .ifPresent(existante -> {


                    boolean memeFournisseur =
                            facture.getFournisseur() != null &&
                                    existante.getFournisseur() != null &&
                                    facture.getFournisseur()
                                            .equalsIgnoreCase(
                                                    existante.getFournisseur()
                                            );


                    if (memeFournisseur ||
                            facture.getFournisseur() == null) {

                        throw new RegleMetierException(
                                "Cette facture existe déjà : "
                                        + facture.getNumeroFacture()
                        );
                    }
                });
    }



    private void appliquerChamp(Facture facture, ChampExtrait champ) {

        if (champ.getNom().startsWith("_alt_")) {
            return;
        }


        switch (champ.getNom()) {

            case "numeroFacture" ->
                    facture.setNumeroFacture(champ.getValeur());


            case "fournisseur" ->
                    facture.setFournisseur(champ.getValeur());


            case "mfFournisseur" ->
                    facture.setMfFournisseur(champ.getValeur());


            case "dateFacture" ->
                    facture.setDateFacture(
                            parserDateFr(champ.getValeur())
                    );


            case "dateEcheance" ->
                    facture.setDateEcheance(
                            parserDateFr(champ.getValeur())
                    );


            case "montantHT" ->
                    facture.setMontantHT(
                            parserMontantFr(champ.getValeur())
                    );


            case "montantTVA" ->
                    facture.setMontantTVA(
                            parserMontantFr(champ.getValeur())
                    );


            case "montantTTC" ->
                    facture.setMontantTTC(
                            parserMontantFr(champ.getValeur())
                    );


            case "devise" ->
                    facture.setDevise(champ.getValeur());


            case "bonDeCommande" ->
                    facture.setBonDeCommande(champ.getValeur());
        }
    }



    private LocalDate parserDateFr(String texte) {

        try {

            return LocalDate.parse(
                    texte,
                    FORMAT_DATE_FR
            );

        } catch(Exception e){

            return null;
        }
    }



    private BigDecimal parserMontantFr(String texte){

        try {

            String valeur =
                    texte.replace(" ","")
                            .replace(",", ".");


            return new BigDecimal(valeur);


        } catch(Exception e){

            return null;
        }
    }



    public Facture obtenirParId(Long id){

        return factureRepository.findById(id)

                .orElseThrow(() ->
                        new RessourceNonTrouveeException(
                                "Facture introuvable avec id : "
                                        + id
                        )
                );
    }



    @Transactional
    public Facture confirmerReception(
            Long id,
            String usernameIt){

        Facture facture =
                obtenirParId(id);


        facture.setReceptionConfirmee(true);
        facture.setConfirmeePar(usernameIt);
        facture.setDateConfirmationReception(
                LocalDateTime.now()
        );


        return factureRepository.save(facture);
    }



    public List<Facture> listerAvecFiltres(
            StatutFacture statut,
            LocalDate dateDebut,
            LocalDate dateFin){


        return factureRepository.findAll()
                .stream()

                .filter(f ->
                        statut == null ||
                                f.getStatut() == statut)

                .filter(f ->
                        dateDebut == null ||
                                (f.getDateFacture()!=null &&
                                        !f.getDateFacture()
                                                .isBefore(dateDebut)))

                .filter(f ->
                        dateFin == null ||
                                (f.getDateFacture()!=null &&
                                        !f.getDateFacture()
                                                .isAfter(dateFin)))

                .toList();
    }



    public List<Facture> listerToutes(){

        return factureRepository.findAll();
    }



    @Transactional
    public Facture mettreAJour(
            Long id,
            Facture donnees){


        Facture facture =
                obtenirParId(id);



        facture.setFournisseur(
                donnees.getFournisseur()
        );

        facture.setMfFournisseur(
                donnees.getMfFournisseur()
        );

        facture.setNumeroFacture(
                donnees.getNumeroFacture()
        );

        facture.setDateFacture(
                donnees.getDateFacture()
        );

        facture.setDateEcheance(
                donnees.getDateEcheance()
        );

        facture.setBonDeCommande(
                donnees.getBonDeCommande()
        );

        facture.setMontantHT(
                donnees.getMontantHT()
        );

        facture.setMontantTVA(
                donnees.getMontantTVA()
        );

        facture.setMontantTTC(
                donnees.getMontantTTC()
        );

        facture.setDevise(
                donnees.getDevise()
        );

        facture.setObservations(
                donnees.getObservations()
        );


        return factureRepository.save(facture);
    }



    @Transactional
    public Facture valider(
            Long id,
            String username){


        Facture facture =
                obtenirParId(id);


        if(facture.getStatut()
                == StatutFacture.REJETEE){

            throw new RegleMetierException(
                    "Impossible de valider une facture rejetée."
            );
        }


        facture.setStatut(
                StatutFacture.VALIDEE
        );

        facture.setValidePar(username);

        facture.setDateValidation(
                LocalDateTime.now()
        );


        return factureRepository.save(facture);
    }



    @Transactional
    public Facture rejeter(
            Long id,
            String username){


        Facture facture =
                obtenirParId(id);


        if(facture.getStatut()
                == StatutFacture.VALIDEE){

            throw new RegleMetierException(
                    "Impossible de rejeter une facture validée."
            );
        }


        facture.setStatut(
                StatutFacture.REJETEE
        );

        facture.setValidePar(username);

        facture.setDateValidation(
                LocalDateTime.now()
        );


        return factureRepository.save(facture);
    }



    @Transactional
    public void supprimer(Long id){

        Facture facture =
                obtenirParId(id);

        factureRepository.delete(facture);
    }
}