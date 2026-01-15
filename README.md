# 🎬 CineMatch

Έξυπνη πλατφόρμα για λάτρεις του κινηματογράφου (Web App).  
**Backend:** Spring Boot (Java 17) • **Frontend:** React + TypeScript • **DB:** PostgreSQL (Docker)

---

## ✨ Περιγραφή
Το **CineMatch** είναι web εφαρμογή που συνδυάζει κινηματογραφικό περιεχόμενο με social & AI λειτουργίες:
- Αναζήτηση και προβολή πληροφοριών για ταινίες / actors / directors (TMDb)
- Authentication (Register / Login με JWT)
- Quiz/Trivia με βαθμολογία
- Social feed / posts (images/videos), likes/comments
- AI features (π.χ. quiz questions / recast-it / sentiment) μέσω εξωτερικών APIs

---

## 🧰 Τεχνολογίες
- Backend: **Java + Spring Boot**
- Database: **PostgreSQL**
- Frontend: **React + TypeScript**
- Testing: **JUnit + Mockito**
- CI/CD: **GitHub Actions** (build + test)
- Containers: **Docker / docker-compose** (μόνο για DB)
- APIs: **TMDb**, **HuggingFace** (όπου απαιτείται)
- Version Control: **Git / GitHub**

---

## 📦 Δομή Project
- `.github/workflows/` → GitHub Actions CI
- `backend/` → Spring Boot API
- `frontend/cinematch-frontend/` → React app
- `docker/` → docker-compose για PostgreSQL + pgAdmin
- `docs/research/` → τεκμηρίωση/έρευνα (CI/CD κλπ)

---

## ✅ Προαπαιτούμενα
- **Java 17**
- **Node.js (LTS)**
- **Docker Desktop**
- (Προαιρετικά) IntelliJ IDEA / VS Code

---

## 🔐 Environment Variables (.env)
Για να δουλέψουν τα external APIs και το JWT χρειάζεται `.env` αρχείο στο **repo root** (ή/και στο `backend/`).

Παράδειγμα:
```env
TMDB_API_KEY=...
TMDB_ACCESS_TOKEN=...
JWT_SECRET=...
HUGGINGFACE_API_KEY=..
```

---

## ✅ Build / Run Instructions (Local)

```bash
git clone <repo>
cd CineMatch

# 1) DB
cd docker
docker compose up -d
cd ..

# 2) Backend
cd backend
./mvnw spring-boot:run
cd ..

# 3) Frontend
cd frontend/cinematch-frontend
npm install
npm run dev
```
