import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';

export default function Header({ onSearch, onLoginClick, onRegisterClick, onCartClick }) {
  const { user, isLoggedIn, loading, logout } = useAuth();
  const { cartCount, loading: cartLoading } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  

  const isMyProductsPage = location.pathname === "/my-seller";
  const isProfilePage = location.pathname.startsWith("/profile"); 

 
  const handleLogout = async () => {
    console.log('開始登出...');
    try {
      await logout();
      console.log('登出完成，購物車將自動清空');
     
      navigate("/");
    } catch (error) {
      console.error('登出失敗:', error);
     
      navigate("/");
    }
  };

  const handleCartClick = () => {
    console.log('購物車點擊 - User:', user, 'Loading:', loading);
    
    if (loading) {
      alert("載入中，請稍後再試");
      return;
    }
    
    if (!isLoggedIn || !user?.userId) {
      alert("請先登入才能查看購物車");
      return;
    }
    
    if (onCartClick) {
      onCartClick();
    }
  };

  const handleMyProductsClick = () => {
    if (!isLoggedIn) {
      alert("請先登入才能查看我的賣場");
      return;
    }
    if (!isMyProductsPage) {
      navigate('/my-seller');
    }
  };

  
  const handleProfileClick = () => {
    if (!isLoggedIn) {
      alert("請先登入才能查看個人中心");
      return;
    }
    if (!isProfilePage) {
      navigate('/profile');
    }
  };

  if (loading) {
    return (
      <header className="header">
        <div>載入中...</div>
      </header>
    );
  }

  return (
    <header className="header">
      <div className="header-left">
        <div className="logo" onClick={() => navigate("/")} style={{ cursor: 'pointer' }}>
          競好購
        </div>
        {isLoggedIn && (
          <span className="welcome-text">歡迎，{user?.username || '用戶'}</span>
        )}
      </div>
      <div className="header-center">
        <div className="search-bar">
          <input 
            className="search-input"
            placeholder="搜尋商品..."
            onChange={e => onSearch && onSearch(e.target.value)}
          />
          <button className="search-btn">🔍</button>
        </div>
      </div>
      <nav className="nav-section">
        {isLoggedIn ? (
          <div className="user-menu">
           
            <button 
              className={`nav-btn ${isProfilePage ? 'nav-btn-active' : ''}`}
              onClick={handleProfileClick}
              disabled={isProfilePage}
            >
              個人中心
            </button>
            
         
            <button 
              className={`nav-btn ${isMyProductsPage ? 'nav-btn-active' : ''}`}
              onClick={handleMyProductsClick}
              disabled={isMyProductsPage}
            >
              我的賣場
            </button>
            
         
            <button className="logout-btn" onClick={handleLogout}>
              登出
            </button>
          </div>
        ) : (
          <div className="auth-buttons">
            <button className="login-btn" onClick={onLoginClick}>登入</button>
            <button className="register-btn" onClick={onRegisterClick}>註冊</button>
          </div>
        )}
        <div className="cart-btn-wrapper">
          <button className="cart-btn" onClick={handleCartClick}>🛒</button>
          {cartCount > 0 && (
            <span className="cart-badge">
              {cartLoading ? '...' : cartCount}
            </span>
          )}
        </div>
      </nav>
    </header>
  );
}
