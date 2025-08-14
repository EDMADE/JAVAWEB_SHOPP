import React from "react";
import { useCart } from '../contexts/CartContext';
import { useNavigate } from "react-router-dom";
import './CartPage.css';

const API_BASE_URL = 'http://localhost:8080';

function CartPage({ onClose }) {
  const { 
    cartItems, 
    cartCount, 
    loading, 
    error, 
    fetchCart, 
    updateCartItem,  
    removeCartItem 
  } = useCart();
  
  const navigate = useNavigate();

 
  const getImageUrl = (imageUrl) => {
    if (!imageUrl) return "/images/no-image.png";
    return imageUrl.startsWith('/') ? API_BASE_URL + imageUrl : imageUrl;
  };

  const formatPrice = (price) => {
    return `NT$ ${Number(price || 0).toLocaleString()}`;
  };

  const formatSkuSpec = (specJson) => {
    if (!specJson) return '';
    try {
      const spec = typeof specJson === 'string' ? JSON.parse(specJson) : specJson;
      return Object.entries(spec).map(([key, value]) => `${key}: ${value}`).join(', ');
    } catch {
      return specJson;
    }
  };

  const handleCheckout = () => {
    if (cartItems.length === 0) {
      alert('購物車中沒有商品');
      return;
    }
    navigate('/checkout');
    if (onClose) onClose();
  };

  
  if (loading) {
    return (
      <div className="cart-shopee">
        <div className="cart-header">
          <h2 className="cart-title">我的購物車</h2>
          <button className="cart-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cart-loading">
          <div className="loading-spinner"></div>
          <p>載入中...</p>
        </div>
      </div>
    );
  }


  if (error) {
    return (
      <div className="cart-shopee">
        <div className="cart-header">
          <h2 className="cart-title">我的購物車</h2>
          <button className="cart-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cart-error">
          <p>{error}</p>
          <div>
            <button className="cart-btn" onClick={fetchCart} style={{marginRight: '10px'}}>
              重試
            </button>
            <button className="cart-btn" onClick={onClose}>
              關閉
            </button>
          </div>
        </div>
      </div>
    );
  }

 
  if (cartItems.length === 0) {
    return ( 
      <div className="cart-shopee">
        <div className="cart-header">
          <h2 className="cart-title">我的購物車</h2>
          <button className="cart-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cart-empty">
          <div className="empty-icon">🛒</div>
          <p className="empty-text">購物車是空的</p>
          <button onClick={onClose} className="cart-btn cart-btn-primary">
            繼續購物
          </button> 
        </div>
      </div>
    );
  }


  const totalAmount = cartItems.reduce((sum, item) => {
    const price = Number(item?.price || 0);
    const quantity = Number(item?.quantity || 0);
    return sum + (price * quantity);
  }, 0);

  return (
    <div className="cart-shopee">
    
      <div className="cart-header">
        <h2 className="cart-title">我的購物車</h2>
        <button className="cart-close-btn" onClick={onClose}>✕</button>
      </div>

    
      <div className="cart-content">
        <div className="cart-items-list">
          {cartItems.map(item => (
            <div key={item.cartItemId} className="cart-item-card">
          
              <div className="cart-item-image">
                <img 
                  src={getImageUrl(item.productImage)} 
                  alt={item.productName || '商品'} 
                />
              </div>

          
              <div className="cart-item-info">
                <div className="cart-item-main-info">
                  <h3 className="cart-item-name">
                    {item.productName || '未知商品'}
                  </h3>
                  {item.skuSpec && (
                    <div className="cart-item-spec">
                      規格：{formatSkuSpec(item.skuSpec)}
                    </div>
                  )}
                  
                  <div className="cart-item-price">
                    {formatPrice(item.price)}
                  </div>
                </div>

                
                <div className="cart-item-actions">
                  <div className="quantity-selector">
                    <button 
                      className="qty-btn qty-decrease"
                      onClick={() => updateCartItem(item.cartItemId, (item.quantity || 1) - 1)}
                      disabled={item.quantity <= 1}
                    >
                      −
                    </button>
                    <input
                      type="number"
                      className="qty-input"
                      min="1"
                      value={item.quantity || 1}
                      onChange={e => updateCartItem(item.cartItemId, parseInt(e.target.value) || 1)}
                    />
                    <button 
                      className="qty-btn qty-increase"
                      onClick={() => updateCartItem(item.cartItemId, (item.quantity || 0) + 1)}
                    >
                      +
                    </button>
                  </div>

                  <div className="cart-item-subtotal">
                    {formatPrice(Number(item.price || 0) * Number(item.quantity || 0))}
                  </div>

                  <button 
                    className="remove-btn"
                    onClick={() => removeCartItem(item.cartItemId)}
                    title="移除商品"
                  >
                    🗑️
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

    
      <div className="cart-footer">
        <div className="cart-summary">
          <div className="total-info">
            <span className="total-label">
              總金額（{cartItems.length}件）：
            </span>
            <span className="total-amount">{formatPrice(totalAmount)}</span>
          </div>
          <div className="cart-buttons">
            <button className="cart-btn cart-btn-secondary" onClick={onClose}>
              繼續購物
            </button>
            <button 
              className="cart-btn cart-btn-primary" 
              onClick={handleCheckout}
            >
              去結帳 ({cartItems.length})
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartPage;
