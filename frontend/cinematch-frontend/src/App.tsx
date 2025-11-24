import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import HomePage from "./pages/HomePage";
import MovieDetailsPage from "./pages/MovieDetailsPage";
import SearchPage from "./pages/SearchPage";
import TrendingPage from "./pages/TrendingPage";
import EditProfilePage from "./pages/EditProfilePage";
import Navbar from "./components/Navbar"; // <<< προστέθηκε
import ProfilePage from "./pages/ProfilePage";
import LoginPage from "./pages/LoginPage";       // <<< ΝΕΟ
import RegisterPage from "./pages/RegisterPage";

function App() {
  return (
    <BrowserRouter>

      {/* NAVIGATION BAR */}
      <Navbar />

      {/* MAIN CONTENT (ώστε να μην καλύπτεται από το navbar) */}
      <div style={{ paddingTop: "100px" }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/movie/:id" element={<MovieDetailsPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/trending" element={<TrendingPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/profile/edit" element={<EditProfilePage />} />
            {/* AUTH ROUTES */}
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </div>

    </BrowserRouter>
  );
}

export default App;