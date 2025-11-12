# 🧩 Research #1 – TMDb & Εναλλακτικά APIs (OMDb, IMDb)

## 🎯 Στόχος
Να διερευνηθούν διαθέσιμα **Movie APIs** που παρέχουν δεδομένα για ταινίες, ηθοποιούς, σκηνοθέτες, βαθμολογίες, trending λίστες και αναζητήσεις.  
Κύριος στόχος είναι να επιλεγεί το API που θα χρησιμοποιηθεί στο **CineMatch Web App** για:
- Αναζήτηση και προβολή ταινιών
- Προβολή πληροφοριών για ηθοποιούς/σκηνοθέτες
- Απόκτηση στατιστικών (π.χ. δημοτικότητα, βραβεία, βαθμολογίες)
- Σύνδεση με τα KPIs του MVP (Star Power Index, Audience Engagement Score)

---

## 🔍 Υποψήφια APIs

### 1️⃣ TMDb (The Movie Database)
- **Website:** [https://www.themoviedb.org](https://www.themoviedb.org)
- **API Docs:** [https://developer.themoviedb.org/reference](https://developer.themoviedb.org/reference)
- **Authentication:** API key (δωρεάν), υποστηρίζει OAuth 2.0 για advanced use.
- **Format:** JSON
- **Endpoints (ενδεικτικά):**
    - `/movie/popular` – Δημοφιλείς ταινίες
    - `/movie/{id}` – Πληροφορίες για συγκεκριμένη ταινία
    - `/person/{id}` – Πληροφορίες για ηθοποιό
    - `/search/movie` – Αναζήτηση ταινίας
    - `/trending/{media_type}/{time_window}` – Trending λίστες
- **Πλεονεκτήματα:**
    - Δωρεάν, αξιόπιστο, ενημερώνεται συχνά
    - Πλούσιο dataset (ταινίες, ηθοποιοί, trailers, εικόνες)
    - Πολύ καλή τεκμηρίωση
- **Μειονεκτήματα:**
    - Όρια rate limit (40 requests / 10s ανά IP)
    - Δεν περιλαμβάνει όλα τα βραβεία ή box office data

---

### 2️⃣ OMDb (Open Movie Database)
- **Website:** [https://www.omdbapi.com](https://www.omdbapi.com)
- **API Docs:** [https://www.omdbapi.com](https://www.omdbapi.com)
- **Authentication:** API key (δωρεάν για περιορισμένη χρήση)
- **Format:** JSON
- **Endpoints (ενδεικτικά):**
    - `/?t=<title>` – Αναζήτηση με τίτλο
    - `/?i=<imdbID>` – Λεπτομέρειες με IMDb ID
- **Πλεονεκτήματα:**
    - Ελαφρύ και απλό στη χρήση
    - Περιλαμβάνει IMDb ratings
- **Μειονεκτήματα:**
    - Περιορισμένα δεδομένα (όχι images ή media assets)
    - Όχι κατάλληλο για πλήρη εμπειρία CineMatch (μόνο text data)

---

### 3️⃣ IMDb API (Unofficial / RapidAPI)
- **Website:** [https://rapidapi.com/apidojo/api/imdb8](https://rapidapi.com/apidojo/api/imdb8)
- **Authentication:** API key μέσω RapidAPI
- **Format:** JSON
- **Endpoints:**
    - `/title/get-top-rated-movies`
    - `/title/get-most-popular-movies`
    - `/title/get-awards`
- **Πλεονεκτήματα:**
    - Πρόσβαση σε IMDb ratings & awards
- **Μειονεκτήματα:**
    - Όχι επίσημο API από IMDb
    - Πιθανό κόστος μέσω RapidAPI
    - Περιορισμοί σε requests

---

## ⚖️ Συγκριτικός Πίνακας

| API       | Δεδομένα | Εικόνες/Trailers | Βραβεία | Δωρεάν | Τεκμηρίωση | Κατάλληλο για MVP |
|------------|-----------|------------------|----------|----------|---------------|--------------------|
| **TMDb**   | ✅ Πλήρη | ✅ Ναι | ⚠️ Μερικά | ✅ Ναι | ✅ Πολύ καλή | ✅ |
| **OMDb**   | ⚠️ Περιορισμένα | ❌ Όχι | ❌ Όχι | ✅ Ναι | ✅ Καλή | ❌ |
| **IMDb (RapidAPI)** | ✅ Καλό | ⚠️ Περιορισμένα | ✅ Ναι | ❌ Όχι | ⚠️ Μέτρια | ⚠️ |

---

## 🧠 Συμπέρασμα
Το **TMDb API** αποτελεί την **βέλτιστη επιλογή** για το CineMatch MVP.  
Παρέχει πλούσια δεδομένα, καλή τεκμηρίωση, δωρεάν χρήση και επαρκή υποστήριξη για όλα τα features του MVP.

---

## ⚙️ Επόμενα Βήματα / Ενσωμάτωση

### 🔹 Backend (Spring Boot)
- Εγγραφή API key στο `.env` ή application.properties
- Δημιουργία service π.χ. `MovieService.java` με κλήσεις προς TMDb REST endpoints
- Υλοποίηση controller endpoints `/api/movies`, `/api/actors`

### 🔹 Frontend (React)
- Fetch δεδομένων από το backend
- Εμφάνιση:
    - Trending movies
    - Search bar (τίτλος)
    - Movie details page (poster, cast, synopsis)
    - Actor profile pages

### 🔹 Παραδείγματα Requests
**GET Trending:**
```bash
GET https://api.themoviedb.org/3/trending/movie/day?api_key=<API_KEY>
