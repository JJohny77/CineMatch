import React from "react";
import { Navigate } from "react-router-dom";

interface ProtectedRouteProps {
  children: JSX.Element;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  // Παίρνουμε το token από το localStorage
  const token = localStorage.getItem("token");

  // Αν δεν υπάρχει token → redirect στο login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // Αν υπάρχει token → επιτρέπουμε πρόσβαση
  return children;
};

export default ProtectedRoute;