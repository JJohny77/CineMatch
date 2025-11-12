# ⚙️ Research #2 – Επιλογή Τεχνολογιών & Αρχιτεκτονικής (React, Spring Boot, PostgreSQL, CI/CD)

## 🎯 Στόχος
Να καθοριστεί η βασική **τεχνολογική στοίβα (Tech Stack)** και η **αρχιτεκτονική** που θα υποστηρίξει την ανάπτυξη της πλατφόρμας **CineMatch**.  
Η επιλογή πρέπει να διασφαλίζει:
- Επεκτασιμότητα και σταθερότητα
- Ευκολία συντήρησης
- Υποστήριξη testing και CI/CD
- Συμβατότητα με APIs, AI services και Docker containers

---

## 🧱 1️⃣ Αρχιτεκτονική Εφαρμογής

### 🔹 Τύπος: **Client–Server (3-Tier Architecture)**
1. **Frontend (Presentation Layer)** → React (JavaScript/TypeScript)
2. **Backend (Application Layer)** → Spring Boot (Java)
3. **Database (Data Layer)** → PostgreSQL

Κάθε επίπεδο είναι ανεξάρτητο, επικοινωνεί μέσω REST APIs και μπορεί να εκτελείται σε δικό του container.

---

## 🖥️ 2️⃣ Frontend – React

| Παράμετρος | Περιγραφή |
|-------------|------------|
| **Framework** | React 18 + Vite |
| **Γλώσσα** | JavaScript / TypeScript |
| **UI Libraries** | Material UI (MUI), TailwindCSS ή ShadCN (light setup) |
| **State Management** | Context API (ή Redux Toolkit αν απαιτηθεί) |
| **Routing** | React Router |
| **API Communication** | Axios ή Fetch API |
| **Testing** | Jest + React Testing Library |
| **Build/Deploy** | Vite build → Docker container (nginx-based) |

### 📦 Παραδοτέα Frontend
- Responsive UI
- Σελίδες:
    - Trending / Search / Movie Details
    - Actor Profiles
    - Quiz “Which Actor Are You?”
- Επικοινωνία με backend endpoints (`/api/movies`, `/api/actors`, `/api/quiz`)

---

## 🧩 3️⃣ Backend – Spring Boot

| Παράμετρος | Περιγραφή |
|-------------|------------|
| **Framework** | Spring Boot 3 (Java 17+) |
| **Dependencies** | Spring Web, Spring Data JPA, Spring Security, Lombok |
| **API Style** | RESTful |
| **Testing** | JUnit 5 + Mockito |
| **Database ORM** | Hibernate |
| **Documentation** | Swagger UI (springdoc-openapi) |
| **Build Tool** | Maven |
| **Environment** | application.properties / `.env` για secrets (API keys, DB credentials) |

### 📦 Παραδοτέα Backend
- Endpoints:
    - `/api/movies` → Λήψη δεδομένων από TMDb API
    - `/api/actors` → Προβολή & αναζήτηση ηθοποιών
    - `/api/ai/sentiment` → AI endpoint (Hugging Face integration)
    - `/api/quiz` → “Which Actor Are You?”
- Ενσωμάτωση με **PostgreSQL** μέσω JPA Repositories

---

## 🗄️ 4️⃣ Database – PostgreSQL

| Παράμετρος | Περιγραφή |
|-------------|------------|
| **DBMS** | PostgreSQL 16 |
| **Tooling** | pgAdmin 4 / DBeaver |
| **Schema Example** | Tables: `movies`, `actors`, `users`, `reviews`, `quiz_results` |
| **Connection** | Spring Boot → JDBC URL |
| **Persistence** | JPA/Hibernate auto-generation μέσω `ddl-auto=update` |

### 💾 Σχέση Backend – Database
Η επικοινωνία γίνεται μέσω Spring Data JPA → Hibernate ORM → PostgreSQL.  
Τα entities χαρτογραφούνται σε πίνακες και υποστηρίζουν queries με JPQL.

---

## 🧰 5️⃣ CI/CD Pipeline (βασικό setup)

| Στάδιο | Εργαλείο | Περιγραφή |
|---------|-----------|------------|
| **Version Control** | Git + GitHub | Διαχείριση branches και pull requests |
| **Build & Test** | Maven + JUnit + GitHub Actions | Αυτόματη εκτέλεση tests σε κάθε push |
| **Containerization** | Docker | Δημιουργία containers για backend & frontend |
| **Deployment** | Docker Compose / Render / Railway | Φορητό deployment περιβάλλον |
| **Secrets Management** | GitHub Secrets | API keys, DB credentials, JWT secrets |

### 📋 Παράδειγμα GitHub Action (Java + Maven)
```yaml
name: Java CI with Maven

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn -B clean verify
```
---
## 🧭 Διάγραμμα Αρχιτεκτονικής Συστήματος

```text
               ┌─────────────────────────────┐
               │   Frontend – React          │
               │   Εφαρμογή Ιστού (UI)       │
               └──────────────┬──────────────┘
                              │
                              │ Κλήσεις REST API
                              ▼
               ┌─────────────────────────────┐
               │   Backend – Spring Boot     │
               │   Επιχειρησιακή Λογική      │
               └──────────────┬──────────────┘
                              │
                              │ Ερωτήματα JPA
                              ▼
               ┌─────────────────────────────┐
               │   Βάση Δεδομένων –          │
               │   PostgreSQL                │
               └──────────────┬──────────────┘
                              │
                              │ Εσωτερικά Δεδομένα
                              ▼
               ┌─────────────────────────────┐
               │   Pipeline CI/CD            │
               │   GitHub Actions + Docker   │
               └─────────────────────────────┘

          ↖──────────────────────────────────────↗
           Εξωτερικά APIs:
           TMDb, Hugging Face, Luxand