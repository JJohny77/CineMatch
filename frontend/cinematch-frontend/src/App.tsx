import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import HomePage from "./pages/HomePage";
import MovieDetailsPage from "./pages/MovieDetailsPage";
import SearchPage from "./pages/SearchPage";
import TrendingPage from "./pages/TrendingPage";


function App() {
  return (
    <BrowserRouter>
      {/* Navigation bar */}
      <nav
        style={{
          width: "100%",
          background: "#111",
          padding: "15px 25px",
          display: "flex",
          gap: "25px",
          position: "fixed",
          top: 0,
          left: 0,
          zIndex: 1000,
        }}
      >
        <Link to="/" style={{ color: "white", fontSize: "18px", textDecoration: "none" }}>
          Home
        </Link>

        <Link to="/search" style={{ color: "white", fontSize: "18px", textDecoration: "none" }}>
          Search
        </Link>

        <Link to="/trending" style={{ color: "white", fontSize: "18px", textDecoration: "none" }}>
          Trending
        </Link>
      </nav>

      {/* MAIN CONTENT (keeps everything below the navbar) */}
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
