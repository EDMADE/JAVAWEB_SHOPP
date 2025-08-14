import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import HomePage from './pages/HomePage';
import ProductDetail from './components/ProductDetail';
import CartPage from './components/CartPage';
import ErrorBoundary from './components/ErrorBoundary';
import MySeller from './pages/MySeller';
import AddProduct from './pages/AddProduct';
import EditProduct from './pages/EditProduct';
import ProfilePage from "./pages/ProfilePage";
import CheckoutPage from './pages/CheckoutPage';
import EmailVerification from './components/EmailVerification';
import OrderDetailPage from './pages/OrderDetailPage';
import './App.css';

export default function App() {
  const [showCart, setShowCart] = useState(false);

  const handleCartClick = () => {
    console.log('購物車被點擊');
    setShowCart(true);
  };

  const handleCloseCart = () => {
    setShowCart(false);
  };

  return (
    <ErrorBoundary>
      <AuthProvider>
        <CartProvider>
          <Router>
            <Routes>
              <Route path="/" element={<HomePage onCartClick={handleCartClick} />} />
              <Route path="/product/:id" element={<ProductDetail onCartClick={handleCartClick} />} />
              <Route path="/order-detail/:id" element={<OrderDetailPage />} />
              <Route path="/my-seller" element={<MySeller onCartClick={handleCartClick} />} />
              <Route path="/add-product" element={<AddProduct onCartClick={handleCartClick} />} />
              <Route path="/edit-product/:id" element={<EditProduct onCartClick={handleCartClick} />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
               <Route path="/verify-email/:token" element={<EmailVerification />} />
            </Routes>
            
            {showCart && (
              <div className="cart-overlay">
                <div className="cart-modal">
                  <CartPage onClose={handleCloseCart} />
                </div>
              </div>
            )}
          </Router>
        </CartProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}
