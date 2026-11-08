# Cement Project - Setup and Running Guide

This guide will help you set up and run the Cement Project, which includes a Spring Boot backend and a static HTML/CSS/JS frontend.

## Project Structure

- `front/fronte/` — Frontend application (static HTML/CSS/JS)
- `prefacture/` — Backend Spring Boot application
- `javafx-21_windows-x64_bin-sdk/` — JavaFX SDK (included)
- `logo/` — Project logos and assets

## Prerequisites

Before running the project, ensure you have the following installed:

### Required Software
- **Java Development Kit (JDK) 21** - [Download here](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- **Apache Maven 3.6+** - [Download here](https://maven.apache.org/download.cgi)
- **Python 3.x** (for simple HTTP server) - [Download here](https://www.python.org/downloads/)
- **MySQL** (optional, for production database) - [Download here](https://dev.mysql.com/downloads/mysql/)

### Verification Commands
```powershell
java -version
mvn -version
python --version
```

## Backend Setup (Spring Boot)

### 1. Navigate to Backend Directory
```powershell
cd prefacture
```

### 2. Configure Database and API Keys
The project uses H2 database by default (in-memory). For MySQL, configure in `src/main/resources/application.properties`:

```properties
# MySQL Configuration (optional)
spring.datasource.url=jdbc:mysql://localhost:3306/cement_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Groq API Key (required for OCR functionality)
groq.api.key=YOUR_GROQ_API_KEY_HERE
```

**Important:** You need to:
1. Replace `YOUR_GROQ_API_KEY_HERE` with your actual Groq API key
2. Replace `YOUR_DATABASE_PASSWORD` with your MySQL password if using MySQL
3. Get a Groq API key from https://console.groq.com/

### 3. Build the Project
```powershell
mvn clean install
```

### 4. Run the Backend Server
```powershell
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 5. Access H2 Console (if using H2 database)
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## Frontend Setup

### 1. Navigate to Frontend Directory
```powershell
cd front/fronte
```

### 2. Start Simple HTTP Server
```powershell
python -m http.server 8000
```

### 3. Access the Application
Open your browser and navigate to:
- Main page: `http://localhost:8000/index.html`
- Login page: `http://localhost:8000/login.html`
- Admin login: `http://localhost:8000/admin-login.html`

## Frontend File Inventory

- `index.html` — main entry page (static HTML)
- `login.html` — user login page
- `admin-login.html` — admin login page
- `dashboard.html` — dashboard UI (post-login view)
- `factures.html` — list of invoices view
- `facture.html` — single invoice detail/print view
- `utilisateurs.html` — users management view
- `styles.css` — global stylesheet for the site
- `app.js` — main frontend logic (routing, UI behaviors)
- `api.js` — frontend API helper (AJAX/fetch wrappers)

## API Configuration

The frontend communicates with the backend through the `api.js` file. Ensure the API base URL matches your backend server:

```javascript
// In api.js, check the base URL configuration
const API_BASE_URL = 'http://localhost:8080/api';
```

## Running the Complete Application

### Option 1: Separate Terminals
1. **Terminal 1 (Backend):**
   ```powershell
   cd prefacture
   mvn spring-boot:run
   ```

2. **Terminal 2 (Frontend):**
   ```powershell
   cd front/fronte
   python -m http.server 8000
   ```

### Option 2: Serve Frontend from Backend
To serve the frontend from the Spring Boot backend, configure static resources in your Spring Boot application and move the frontend files to `src/main/resources/static/`.

## Troubleshooting

### Backend Issues
- **Port 8080 already in use:** Change the port in `application.properties`:
  ```properties
  server.port=8081
  ```
- **Maven build fails:** Ensure Java 21 is installed and MAVEN_HOME is configured

### Frontend Issues
- **CORS errors:** Configure CORS in your Spring Boot backend
- **API connection failed:** Check that the backend is running and the API URL in `api.js` is correct

### Database Issues
- **H2 console not accessible:** Ensure `spring-boot-h2console` dependency is included
- **MySQL connection failed:** Verify MySQL server is running and credentials are correct

## Development Notes

- The backend uses Spring Boot 4.0.7 with Java 21
- The frontend is a static SPA-like application with no build tooling
- Security is handled by Spring Security
- OCR functionality is included using Tesseract (tess4j)
- Excel export/import uses Apache POI

## API Documentation

Once the backend is running, access Swagger UI at:
`http://localhost:8080/swagger-ui.html`

---

**Last Updated:** 2026-08-20