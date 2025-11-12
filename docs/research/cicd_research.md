# CineMatch – Μελέτη: CI/CD Pipeline (Maven + GitHub Actions)

## 🎯 Στόχος
Η μελέτη αυτή αφορά τη ρύθμιση ενός απλού αλλά λειτουργικού CI/CD pipeline για το backend του **CineMatch** (Java + Spring Boot), με σκοπό να εξασφαλιστεί ότι κάθε αλλαγή στον κώδικα περνά επιτυχώς από **build** και **testing** πριν γίνει merge στο κύριο branch.

---

## ⚙️ Εργαλεία που εξετάστηκαν

### 1. **GitHub Actions**
- Ενσωματωμένο εργαλείο συνεχούς ολοκλήρωσης (CI) μέσα στο GitHub.
- Δεν απαιτεί εξωτερικό server ή Jenkins.
- Συνδέεται αυτόματα με τα Pull Requests και τα Branch Protection Rules.

### 2. **Maven**
- Εργαλείο build & dependency management για Java.
- Τρέχει τα tests με JUnit/Mockito μέσω της εντολής:
  ```bash
  mvn clean verify

### Πλαίσιο Έργου
- Backend: `backend/` (Spring Boot + Maven)
- Frontend: `frontend/` (React) – δεν χτίζεται σε αυτό το workflow.
- Προστασία κλάδου `main`: απαιτείται passing status check.

## Απαιτήσεις
- `backend/pom.xml` παρών στο repo.
- Tests σε `backend/src/test/java/**`.

## Σημειώσεις
- Το workflow είναι **conditional**: τρέχει μόνο αν υπάρχει `backend/pom.xml`, ώστε να μην αποτυγχάνει πριν ανεβάσουμε backend.
- Όταν προστεθούν integration/UI flows, θα γίνει νέο workflow.