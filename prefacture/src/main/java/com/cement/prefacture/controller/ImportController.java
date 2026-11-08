package com.cement.prefacture.controller;

import com.cement.prefacture.dto.ResumeImport;
import com.cement.prefacture.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * Déclenchement MANUEL de l'import Sage — pour tester sans attendre
 * les 8h du cron, ou pour relancer un import après correction d'un
 * fichier. Réservé à ADMIN (voir SecurityConfig : /import/** ).
 */
@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @Value("${app.sage-import.path}")
    private String cheminFichierSage;

    @PostMapping("/bons-commande")
    public ResponseEntity<ResumeImport> importerMaintenant() {
        ResumeImport resume = importService.importerDepuisFichier(new File(cheminFichierSage));
        return ResponseEntity.ok(resume);
    }
}