import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../utils/api';
import './ProductCard.css';

const API_BASE_URL = 'http://localhost:8080';

export default function ProductCard({ product, updateCartCount, setShowLogin }) {
  const { isLoggedIn, user } = useAuth();
  const [imageError, setImageError] = useState(false);
  const [fallbackError, setFallbackError] = useState(false);
  const [isFavorited, setIsFavorited] = useState(false);
  const [favoriteLoading, setFavoriteLoading] = useState(false);


  useEffect(() => {
    const checkFavoriteStatus = async () => {
      const productId = product.productId || product.id || product.product_id;
      
      if (!productId || !isLoggedIn) {
        setIsFavorited(false);
        return;
      }

      try {
        const savedStatus = localStorage.getItem(`favorite_${productId}`);
        if (savedStatus !== null) {
          setIsFavorited(savedStatus === 'true');
        }

        const response = await api.get(`/api/favorites/${productId}/status`);
        if (response.data.success) {
          const actualStatus = response.data.isFavorited;
          setIsFavorited(actualStatus);
          localStorage.setItem(`favorite_${productId}`, actualStatus.toString());
        }
      } catch (error) {
        console.error('檢查收藏狀態失敗:', error);
      }
    };

    checkFavoriteStatus();
  }, [product.productId, product.id, product.product_id, isLoggedIn, user]);

  
  const handleFavoriteClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isLoggedIn) {
      if (setShowLogin) {
        setShowLogin(true);
      } else {
        alert('請先登入才能收藏商品');
      }
      return;
    }

    const productId = product.productId || product.id || product.product_id;
    if (!productId) {
      console.error('商品 ID 無效');
      return;
    }

    try {
      setFavoriteLoading(true);
      
      if (isFavorited) {
        await api.delete(`/api/favorites/${productId}`);
        setIsFavorited(false);
        localStorage.setItem(`favorite_${productId}`, 'false');
        console.log('✅ 取消收藏成功');
      } else {
        const response = await api.post(`/api/favorites/${productId}`);
        if (response.data.success) {
          setIsFavorited(true);
          localStorage.setItem(`favorite_${productId}`, 'true');
          console.log('✅ 收藏成功');
        } else {
          throw new Error(response.data.message || '收藏失敗');
        }
      }
    } catch (error) {
      console.error('收藏操作失敗:', error);
      
      if (error.response?.status === 401) {
        alert('請重新登入後再試');
        if (setShowLogin) setShowLogin(true);
      } else if (error.response?.status === 400) {
        alert(error.response.data.message || '無法收藏此商品');
      } else {
        alert('收藏操作失敗，請稍後再試');
      }
    } finally {
      setFavoriteLoading(false);
    }
  };


  useEffect(() => {
    if (!isLoggedIn) {
      setIsFavorited(false);
      Object.keys(localStorage).forEach(key => {
        if (key.startsWith('favorite_')) {
          localStorage.removeItem(key);
        }
      });
    }
  }, [isLoggedIn]);


  const getImageUrl = (product) => {
    if (product.productImages && product.productImages.length > 0) {
      const imageUrl = product.productImages[0].imageUrl;
      return imageUrl.startsWith('/') ? API_BASE_URL + imageUrl : imageUrl;
    }
    
    const mainImageUrl = product.mainImageUrl || product.main_image_url;
    if (mainImageUrl) {
      return mainImageUrl.startsWith('/') ? API_BASE_URL + mainImageUrl : mainImageUrl;
    }
    
    return null;
  };

  const handleImageError = (e) => {
    console.log("圖片載入失敗:", e.target.src);
    
    if (!imageError) {
      setImageError(true);
      e.target.src = `${API_BASE_URL}/uploads/default.png`;
    } else if (!fallbackError) {
      setFallbackError(true);
      e.target.src = "/images/no-image.png";
    } else {
      e.target.src = "https://placehold.co/200x200?text=No+Image";
    }
  };


  const isAuctionProduct = () => {
    if (product.status === 'AUCTION') return true;
    if (product.isAuction === true) return true;
    const bidEndTime = product.bid_end_time || product.bidEndTime;
    if (bidEndTime) return true;
    const startPrice = product.start_price || product.startPrice;
    const currentPrice = product.current_price || product.currentPrice;
    if (startPrice && Number(startPrice) > 0 && !currentPrice) return true;
    return false;
  };


  const formatPrice = (product) => {
    if (product.skus && product.skus.length > 0) {
      const prices = product.skus.map(sku => Number(sku.price)).filter(price => price > 0);
      
      if (prices.length > 0) {
        const minPrice = Math.min(...prices);
        const maxPrice = Math.max(...prices);
        
        if (minPrice === maxPrice) {
          return `NT$ ${minPrice.toLocaleString()}`;
        } else {
          return `NT$ ${minPrice.toLocaleString()} - ${maxPrice.toLocaleString()}`;
        }
      }
    }
    
    if (isAuctionProduct()) {
      const startPrice = product.startPrice || product.start_price || 0;
      return `起標價 NT$ ${Number(startPrice).toLocaleString()}`;
    }
    
    const currentPrice = product.currentPrice || product.current_price || 0;
    return `NT$ ${Number(currentPrice).toLocaleString()}`;
  };

  
  const getStockStatus = () => {
    const stock = product.stockQuantity || product.stock_quantity || 0;
    if (stock <= 0) return { text: '缺貨', className: 'out-of-stock' };
    if (stock <= 5) return { text: `剩餘 ${stock} 件`, className: 'low-stock' };
    return { text: `庫存 ${stock} 件`, className: 'in-stock' };
  };


  const getProductStatus = () => {
    const status = product.status;
    const statusMap = {
      'ACTIVE': { text: '販售中', className: 'status-active' },
      'INACTIVE': { text: '已下架', className: 'status-inactive' },
      'AUCTION': { text: '競標中', className: 'status-auction' },
      'AUCTION_ENDED': { text: '競標結束', className: 'status-ended' },
      'SOLD': { text: '已售出', className: 'status-sold' },
      'EXPIRED': { text: '已過期', className: 'status-expired' }
    };
    return statusMap[status] || { text: status, className: 'status-default' };
  };

  
  const getConditionText = () => {
    const condition = product.productCondition || product.product_condition;
    const conditionMap = {
      '全新': '全新',
      '二手':"二手"
    };
    return conditionMap[condition] || condition;
  };

  const imageUrl = getImageUrl(product);
  const stockStatus = getStockStatus();
  const productStatus = getProductStatus();

  return (
    <div className="product-card">
      <Link to={`/product/${product.productId || product.id || product.product_id}`} className="product-link">
        <div className="product-image-container">
          <button 
            className={`favorite-btn ${isFavorited ? 'favorited' : ''}`}
            onClick={handleFavoriteClick}
            disabled={favoriteLoading}
            aria-label={isFavorited ? '取消收藏' : '加入收藏'}
            title={isFavorited ? '取消收藏' : '加入收藏'}
          >
            {favoriteLoading ? (
              <span className="favorite-loading">⟳</span>
            ) : (
              <span className="favorite-icon">
                {isFavorited ? '❤️' : '🤍'}
              </span>
            )}
          </button>

         
          <div className={`product-status-badge ${productStatus.className}`}>
            {productStatus.text}
          </div>

          {imageUrl ? (
            <img 
              src={imageUrl}
              alt={product.name}
              className="product-image"
              onError={handleImageError}
            />
          ) : (
            <div className="no-image-placeholder">
              <span>暫無圖片</span>
            </div>
          )}
        </div>
        
        <div className="product-info">
          <h3 className="product-name">{product.name}</h3>
          
          <div className="product-price">
            {formatPrice(product)}
          </div>

          <div className="product-details">
            <div className={`stock-status ${stockStatus.className}`}>
              📦 {stockStatus.text}
            </div>

            {(product.productCondition || product.product_condition) && (
              <div className="product-condition">
                🏷️ {getConditionText()}
              </div>
            )}

            {product.category && (
              <div className="product-category">
                📂 {product.category}
              </div>
            )}

            {isAuctionProduct() && (product.bidEndTime || product.bid_end_time) && (
              <div className="auction-end-time">
                ⏰ 結束時間：{new Date(product.bidEndTime || product.bid_end_time).toLocaleString('zh-TW')}
              </div>
            )}
          </div>
        </div>
      </Link>
    </div>
  );
}
