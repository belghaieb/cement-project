# Cement Prefacture

Application de pre-facturation ciment avec:
- Backend Spring Boot (Java 21, Maven, MySQL, OCR)
- Frontend statique (HTML, CSS, JavaScript)

## Structure du projet

- `prefacture/` : API backend Spring Boot
- `front/fronte/` : interface web (pages statiques)
- `logo/` : ressources visuelles

## Fonctionnalites principales

- Authentification par session (`/auth/login`, `/auth/logout`, `/auth/me`)
- Gestion des factures (`/factures`)
- Scan OCR de facture (`POST /factures/scan`)
- Ajout de photos de reception (`/factures/{id}/photos`)
- Validation / rejet de facture
- Import manuel Sage (`POST /import/bons-commande`)
- Gestion des utilisateurs admin (`/admin/utilisateurs`)

## Roles

- `ADMIN`
- `RESPONSABLE_FACTURE`
- `IT`

Comptes initiaux (crees si la base est vide):
- `admin / admin123`
- `responsable / resp123`
- `it / it123`

## Prerequis

- JDK 21
- Maven 3.9+
- MySQL 8+
- Tesseract OCR installe (Windows)

## Configuration backend

Fichier: `prefacture/src/main/resources/application.properties`

Verifier au minimum:
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `groq.api.key`
- `ocr.tesseract.data-path`

## Lancer le backend

```powershell
cd prefacture
mvn clean install
mvn spring-boot:run
```

Backend disponible sur:
- `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Lancer le frontend

```powershell
cd front/fronte
python -m http.server 8000
```

Frontend disponible sur:
- `http://localhost:8000/index.html`

Le frontend appelle l'API sur `http://localhost:8080` (voir `front/fronte/api.js`).

## Workflow Git recommande

Depuis la racine du repo:

```powershell
git status
git add README.md front prefacture logo
git status
git commit -m "docs: add project README and update cement prefacture app"
git push origin master
```

## Notes importantes avant push

- Ne pas commiter de secrets reels (API keys, mots de passe).
- Eviter de commiter `prefacture/target/` (deja ignore).
- Verifier les suppressions involontaires avec `git status` avant `git commit`.

## Licence

Usage interne projet ciment (a adapter selon votre politique).
