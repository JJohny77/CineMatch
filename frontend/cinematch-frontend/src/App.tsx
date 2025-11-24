import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";
import MovieDetailsPage from "./pages/MovieDetailsPage";
import SearchPage from "./pages/SearchPage";
import TrendingPage from "./pages/TrendingPage";

import Navbar from "./components/Navbar"; // <<< προστέθηκε

function App() {
  return (
    <BrowserRouter>

      {/* NAVIGATION BAR */}
      <Navbar />

      {/* MAIN CONTENT (ώστε να μην καλύπτεται από το navbar) */}
      <div style={{ paddingTop: "80px" }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/movie/:id" element={<MovieDetailsPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/trending" element={<TrendingPage />} />
        </Routes>
      </div>

    </BrowserRouter>
  );
}

export default App;
