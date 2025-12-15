# Spring Boot

## 1. Τι είναι το Spring Boot;

- Είναι ένα **framework για Java** που μας βοηθάει να φτιάχνουμε:
    - Web εφαρμογές
    - REST APIs
- Βασίζεται πάνω στο **Spring Framework**, αλλά:
    - Απλοποιεί τη ρύθμιση
    - Μειώνει τον “βαρετό” κώδικα υποδομής
    - Μας αφήνει να επικεντρωθούμε στη λογική της εφαρμογής

**Μία φράση:**
> Spring Boot είναι ο γρήγορος και πρακτικός τρόπος να χτίσουμε σύγχρονα backend συστήματα σε Java.

---

## 2. Ποιο πρόβλημα λύνει;

Χωρίς Spring Boot, σε μια Java web εφαρμογή πρέπει να ρυθμιστούν:

- Web server (Tomcat, Jetty κ.λπ.)
- Σύνδεση με βάση δεδομένων
- Security (login, roles)
- Logging, error handling
- Δομή φακέλων και αρχιτεκτονική

Αυτό σημαίνει:
- Περισσότερος χρόνος σε **configuration**
- Περισσότερες πιθανότητες για **λάθη**

Με Spring Boot:

- Πολλά από αυτά γίνονται **αυτόματα**
- Χρειάζεται λιγότερος κώδικας για να πετύχουμε το ίδιο αποτέλεσμα

---

## 3. Κύρια χαρακτηριστικά του Spring Boot

### 3.1 Auto-Configuration
- Αναγνωρίζει τι βιβλιοθήκες υπάρχουν στο project
- Ενεργοποιεί αυτόματα τις αντίστοιχες ρυθμίσεις
- Εμείς ρυθμίζουμε μόνο ό,τι είναι απαραίτητο

### 3.2 Starter Dependencies
- Πακέτα με έτοιμες εξαρτήσεις, π.χ.:
    - `spring-boot-starter-web` (για REST API)
    - `spring-boot-starter-data-jpa` (για βάση δεδομένων)
    - `spring-boot-starter-security` (για authentication/authorization)
- Έτσι αποφεύγεται το ψάξιμο για πολλές βιβλιοθήκες και versions

### 3.3 Embedded Server
- Η εφαρμογή τρέχει σαν **αυτόνομο πρόγραμμα**:
    - `java -jar application.jar`
- Δεν χρειάζεται να εγκατασταθεί ξεχωριστός εφαρμογo-server

---

## 4. Γιατί να επιλέξει κανείς Spring Boot;

### 4.1 Ταχύτητα ανάπτυξης
- Γρήγορη δημιουργία project (Spring Initializr)
- Λιγότερος “boilerplate” κώδικας
- Ιδανικό για:
    - Φοιτητικά projects
    - Πρωτότυπα (prototypes)
    - Πραγματικές εφαρμογές παραγωγής

### 4.2 Καθαρή αρχιτεκτονική
- Ενθαρρύνει δομή όπως:
    - `controller` (REST endpoints)
    - `service` (business logic)
    - `repository` (πρόσβαση στη βάση)
    - `model` (οντότητες)
- Διευκολύνει τη συνεργασία σε ομάδα

### 4.3 Εύκολη σύνδεση με βάση δεδομένων
- Με Spring Data JPA:
    - Ορίζονται entities (π.χ. User, Movie)
    - Δημιουργούνται repositories με ελάχιστο κώδικα
- Το mapping προς τη βάση (π.χ. PostgreSQL) γίνεται με απλές ρυθμίσεις

### 4.4 Security & JWT
- Ενσωμάτωση με Spring Security
- Υποστήριξη:
    - Login / Register
    - Ρόλοι χρηστών (π.χ. USER, ADMIN)
    - JWT tokens για ασφαλή πρόσβαση στα endpoints

---

## 5. Πώς χρησιμοποιείται στο CineMatch;

Στο project **CineMatch**, το Spring Boot χρησιμοποιείται για:

- Δημιουργία REST API για το frontend:
    - `/auth/register`, `/auth/login`
    - `/movies/search`, `/movies/trending`
    - `/quiz/start`, `/quiz/answer`, `/quiz/leaderboard`
- Σύνδεση με **PostgreSQL**:
    - Αποθήκευση χρηστών
    - Αποθήκευση quiz scores
    - Διαχείριση δεδομένων της πλατφόρμας
- Υλοποίηση **business logic**:
    - Κανόνες για το quiz
    - Λογική για KPIs ταινιών
    - Κανόνες πρόσβασης ανά ρόλο χρήστη
- Υποστήριξη **CI/CD & Docker**:
    - Build του backend σε jar
    - Δημιουργία Docker image
    - Εκτέλεση tests μέσω GitHub Actions

---

## 6. Πλεονεκτήματα για την ομάδα και το μάθημα

- Καλύπτει τις τεχνολογικές απαιτήσεις:
    - Java
    - Spring
    - REST
    - Database
    - Testing
    - CI/CD
- Δείχνει:
    - Οργανωμένη αρχιτεκτονική
    - Καλή πρακτική ανάπτυξης λογισμικού
    - Επαγγελματικό επίπεδο backend

---

## 7. Σύνοψη

- Το **Spring Boot** είναι το framework που επιλέχθηκε για το backend του CineMatch.
- Προσφέρει:
    - Γρήγορο στήσιμο
    - Λιγότερο configuration
    - Καθαρή δομή κώδικα
    - Εύκολη συνεργασία με βάση δεδομένων και security
- Χάρη σε αυτό, η ομάδα μπορεί να εστιάσει:
    - Στη λογική της πλατφόρμας
    - Στα features (KPIs, quiz, AI)
    - Στην ποιότητα και στο testing
