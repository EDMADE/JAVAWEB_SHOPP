import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import Header from '../components/Header';
import LoginModal from '../components/LoginModal';
import Footer from '../components/Footer';
import './ProductDetail.css';

const API_BASE_URL = 'http://localhost:8080';

export default function ProductDetail({ onCartClick }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn, user, handleUnauthorized } = useAuth();
  const [product, setProduct] = useState(null);
  const [seller, setSeller] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSpecs, setSelectedSpecs] = useState({});
  const [selectedSku, setSelectedSku] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [showLogin, setShowLogin] = useState(false);
  const [loginTab, setLoginTab] = useState('login');
  const [auctionInfo, setAuctionInfo] = useState(null);
  const [bidAmount, setBidAmount] = useState("");
  const [recentBids, setRecentBids] = useState([]);
  const [timer, setTimer] = useState("");
   const { addToCart, cartCount } = useCart();

  const isAuctionProduct = () => {
    return auctionInfo?.isAuction === true;
  };

  const isAuctionEnded = () => {
    if (!isAuctionProduct()) return false;
    return auctionInfo?.isEnded === true || timer === "已結束";
  };

  useEffect(() => {
    setLoading(true);
    fetch(`${API_BASE_URL}/api/products/${id}`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setProduct(data);
        if (data.skus && data.skus.length > 0) {
          const firstSpec = typeof data.skus[0].specJson === 'string'
            ? JSON.parse(data.skus[0].specJson)
            : data.skus[0].specJson;
          setSelectedSpecs(firstSpec);
          setSelectedSku(data.skus[0]);
        }
        if (data.sellerId) {
          fetch(`${API_BASE_URL}/api/users/${data.sellerId}`, { credentials: 'include' })
            .then(res => res.ok ? res.json() : Promise.reject(res))
            .then(setSeller)
            .catch(() => setSeller({ username: `用戶${data.sellerId}` }));
        }
      })
      .catch(() => setProduct(null))
      .finally(() => setLoading(false));
  }, [id, handleUnauthorized]);

  useEffect(() => {
    if (!product) return;
    
    console.log('載入競標資訊，商品ID:', product.id);
    
    fetch(`${API_BASE_URL}/api/auction/${product.id}`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log('競標資訊:', data);
        setAuctionInfo(data);
        setRecentBids(data.recentBids || []);
      })
      .catch(error => {
        console.error('載入競標資訊失敗:', error);
      });
  }, [product]);

  useEffect(() => {
    if (!auctionInfo?.endTime) return;
    
    const interval = setInterval(() => {
      const end = new Date(auctionInfo.endTime).getTime();
      const now = Date.now();
      let diff = end - now;
      
      if (diff <= 0) {
        setTimer("已結束");
        clearInterval(interval);
        if (auctionInfo && !auctionInfo.isEnded) {
          fetch(`${API_BASE_URL}/api/auction/${product.id}`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
              setAuctionInfo(data);
              setRecentBids(data.recentBids || []);
            });
        }
      } else {
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        diff -= days * (1000 * 60 * 60 * 24);
        const hours = Math.floor(diff / (1000 * 60 * 60));
        diff -= hours * (1000 * 60 * 60);
        const minutes = Math.floor(diff / (1000 * 60));
        diff -= minutes * (1000 * 60);
        const seconds = Math.floor(diff / 1000);
        setTimer(
          `${days}天 ${hours.toString().padStart(2, "0")}:` +
          `${minutes.toString().padStart(2, "0")}:` +
          `${seconds.toString().padStart(2, "0")}`
        );
      }
    }, 1000);
    
    return () => clearInterval(interval);
  }, [auctionInfo, product]);

  useEffect(() => {
    if (!isAuctionProduct() || isAuctionEnded()) return;
    
    const interval = setInterval(() => {
      fetch(`${API_BASE_URL}/api/auction/${product.id}`, { credentials: 'include' })
        .then(res => res.json())
        .then(data => {
          setAuctionInfo(data);
          setRecentBids(data.recentBids || []);
        })
        .catch(error => {
          console.error('刷新競標資訊失敗:', error);
        });
    }, 5000);
    
    return () => clearInterval(interval);
  }, [product, auctionInfo]);

  const isSeller = () => {
    return isLoggedIn && user && product && user.userId === product.sellerId;
  };

  const handleBid = async () => {
    if (!isLoggedIn) {
      setShowLogin(true);
      return;
    }
    
    if (isSeller()) {
      alert("賣家不能對自己的商品出價");
      return;
    }
    
    if (isAuctionEnded()) {
      alert("競標已結束");
      return;
    }
    
const bidAmountNum = Number(bidAmount);
    if (!bidAmount || isNaN(bidAmountNum) || bidAmountNum <= (auctionInfo?.currentPrice || 0)) {
      alert(`請輸入高於目前最高價 NT$ ${(auctionInfo?.currentPrice || 0).toLocaleString()} 的金額`);
      return;
    }
    
    try {
      const res = await fetch(`${API_BASE_URL}/api/auction/${product.id}/bid`, {
        method: "POST",
        credentials: 'include', 
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount: bidAmountNum })
      });
      
      const data = await res.json();
      
      if (res.ok) {
        setBidAmount("");
        const refreshRes = await fetch(`${API_BASE_URL}/api/auction/${product.id}`, { credentials: 'include' });
        const refreshData = await refreshRes.json();
        setAuctionInfo(refreshData);
        setRecentBids(refreshData.recentBids || []);
        alert("出價成功！");
      } else {
        if (data.error?.includes("賣家不能對自己的商品出價")) {
          alert("賣家不能對自己的商品出價");
        } else {
          alert(data.error || "出價失敗");
        }
      }
    } catch (error) {
      console.error('出價錯誤:', error);
      alert("網路錯誤，請稍後再試");
    }
  };

  const handleBuyNow = async () => {
    if (!isLoggedIn) {
      setShowLogin(true);
      return;
    }
    
    if (isSeller()) {
      alert("賣家不能購買自己的商品");
      return;
    }
    
    if (isAuctionProduct() && !isAuctionEnded()) {
      if (confirm(`確認以直購價 NT$ ${product.currentPrice.toLocaleString()} 購買此商品？這將結束競標。`)) {
        try {
          const res = await fetch(`${API_BASE_URL}/api/auction/${product.id}/buy-now`, {
            method: "POST",
            credentials: 'include',
            headers: { "Content-Type": "application/json" }
          });
          
          if (res.ok) {
            alert("直購成功！競標已結束。");
            window.location.reload();
          } else {
            const errorData = await res.json();
            alert(errorData.error || "直購失敗");
          }
        } catch (error) {
          alert("網路錯誤，請稍後再試");
        }
      }
    } else {
      if (stock < 1) {
        alert('此規格已售完');
        return;
      }
      alert(`購買 ${product.name}，數量：${quantity}`);
    }
     navigate("/checkout", {
    state: {
      items: [
        {
          productId: product.id,
          name: product.name,
          price: price,
          quantity: quantity,
           imageUrl: product.mainImageUrl,
          skuId: selectedSku?.skuId,
        
        }
      ]
    }
  });
  };

  const getProductImages = () => {
    if (product?.productImages && product.productImages.length > 0) {
      return product.productImages.map(img => img.imageUrl);
    } else if (product?.mainImageUrl) {
      return [product.mainImageUrl];
    } else {
      return ["https://placehold.co/400x400?text=No+Image"];
    }
  };

  const handleAddToCart = async () => {
    if (!isLoggedIn) {
      setShowLogin(true);
      return;
    }
    
    
    if (isAuctionProduct()) {
      alert('競標商品無法加入購物車，請直接出價或使用直購');
      return;
    }
    
    if (stock < 1) {
      alert('此規格已售完');
      return;
    }
    
    try {
      const requestData = {
        productId: product.id,
        quantity,
        skuId: selectedSku?.skuId || selectedSku?.sku_id
      };
      
      await addToCart(requestData);
      alert('已加入購物車！');
      
    } catch (error) {
      console.error('加入購物車失敗:', error);
      if (error.response?.status === 401) {
        handleUnauthorized();
        setShowLogin(true);
      } else {
        alert('加入購物車失敗：' + (error.message || '未知錯誤'));
      }
    }
  };

  const handleLoginClick = () => { setLoginTab('login'); setShowLogin(true); };
  const handleRegisterClick = () => { setLoginTab('register'); setShowLogin(true); };
  const handleCloseModal = () => { setShowLogin(false); setTimeout(() => setLoginTab('login'), 200); };

  let specs = [];
  try { specs = product?.specifications ? JSON.parse(product.specifications) : []; } catch {}

  useEffect(() => {
    if (!product?.skus || Object.keys(selectedSpecs).length === 0) return;
    const sku = product.skus.find(sku => {
      try {
        const specObj = typeof sku.specJson === 'string' ? JSON.parse(sku.specJson) : sku.specJson;
        return Object.entries(selectedSpecs).every(([k, v]) => specObj[k] === v);
      } catch { return false; }
    });
    setSelectedSku(sku || null);
    setQuantity(1);
  }, [selectedSpecs, product]);

  const images = getProductImages();
  const price = selectedSku ? selectedSku.price : (product?.currentPrice || 0);
  const stock = selectedSku ? selectedSku.stock : (product?.stockQuantity || 0);

  const isOptionSelected = (specName, option) => selectedSpecs[specName] === option;
  const isOptionOutOfStock = (specName, option) => {
    if (!product?.skus) return true;
    const testSpecs = { ...selectedSpecs, [specName]: option };
    if (Object.keys(testSpecs).length !== specs.length) return false;
    const sku = product.skus.find(sku => {
      try {
        const specObj = typeof sku.specJson === 'string' ? JSON.parse(sku.specJson) : sku.specJson;
        return Object.entries(testSpecs).every(([k, v]) => specObj[k] === v);
      } catch { return false; }
    });
    return !sku || sku.stock <= 0;
  };

  const handleSpecChange = (specName, option) => {
    setSelectedSpecs(prev => ({ ...prev, [specName]: option }));
  };

  const handleQuantityChange = (val) => {
    let q = Number(val);
    if (isNaN(q) || q < 1) q = 1;
    if (q > stock) q = stock;
    setQuantity(q);
  };

  if (loading) return <div className="product-container">載入中...</div>;
  if (!product) return <div className="product-container">找不到商品</div>;

  return (
    <div className="product-container">
      <Header 
        onSearch={()=>{}} 
        onLoginClick={handleLoginClick} 
        onRegisterClick={handleRegisterClick} 
        onCartClick={onCartClick} 
      />
      <LoginModal visible={showLogin} onClose={handleCloseModal} defaultTab={loginTab} />
      
      <div className="breadcrumb-nav">
        <span onClick={() => navigate('/')} className="breadcrumb-link">首頁</span>
        <span className="breadcrumb-separator">›</span>
        <span className="breadcrumb-link">{product.category}</span>
        <span className="breadcrumb-separator">›</span>
        <span className="breadcrumb-current">{product.name}</span>
      </div>
      
      <div className="product-layout">
        <div className="product-images-section">
          <div className="main-image-wrapper">
            <img
              className="main-product-image"
              src={images[selectedImage].startsWith('/') ? API_BASE_URL + images[selectedImage] : images[selectedImage]}
              alt={product.name}
              onError={e => e.target.src = "https://placehold.co/400x400?text=No+Image"}
            />
          </div>
          {images.length > 1 && (
            <div className="thumbnail-gallery">
              {images.map((img, i) => (
                <img
                  key={i}
                  src={img.startsWith('/') ? API_BASE_URL + img : img}
                  alt={`縮圖${i + 1}`}
                  className={`thumbnail-image ${selectedImage === i ? 'active' : ''}`}
                  onClick={() => setSelectedImage(i)}
                  onError={e => e.target.src = "https://placehold.co/60x60?text=No+Image"}
                />
              ))}
            </div>
          )}
          <div className="product-code">商品編號：PROD{product.id.toString().padStart(6, '0')}</div>
        </div>
        
        <div className="product-info-section">
          <h1 className="product-name1">{product.name}</h1>
          
          <div className="price-section">
            {isAuctionProduct() ? (
              <div className="auction-price-info">
                <div className="auction-current-price">
                  目前最高價：<span className="price-highlight">NT$ {Number(auctionInfo?.currentPrice || 0).toLocaleString()}</span>
                </div>
                {product.currentPrice && (
                  <div className="auction-buy-now-price">
                    直購價：<span className="buy-now-price">NT$ {Number(product.currentPrice).toLocaleString()}</span>
                  </div>
                )}
              </div>
            ) : (
              <div className="current-price">NT$ {Number(price).toLocaleString()}</div>
            )}
            
            {!isAuctionProduct() && (
              <div className="stock-info">
                <span className={stock > 0 ? 'in-stock' : 'out-of-stock'}>
                  {stock > 0 ? `現貨 ${stock} 件` : "缺貨"}
                </span>
              </div>
            )}
          </div>

          <div className="seller-info">
            <span className="seller-label">賣家：</span>
            <span className="seller-name">{seller ? seller.username : `用戶${product.sellerId}`}</span>
          </div>

          {isAuctionProduct() && (
            <div className="auction-section">
              <h3 className={`auction-title ${isAuctionEnded() ? 'ended' : 'active'}`}>
                🔨 {isAuctionEnded() ? '競標已結束' : '競標中'}
              </h3>
              
              <div className="auction-info">
                <div className="auction-stats">
                  <div className="auction-stat">
                    <span className="stat-label">剩餘時間：</span>
                    <span className={`stat-value ${isAuctionEnded() ? 'ended' : ''}`}>{timer}</span>
                  </div>
                  <div className="auction-stat">
                    <span className="stat-label">出價次數：</span>
                    <span className="stat-value">{auctionInfo?.bidCount || 0}</span>
                  </div>
                  {auctionInfo?.currentWinner && (
                    <div className="auction-stat">
                      <span className="stat-label">目前領先：</span>
                      <span className="stat-value">
                        {isLoggedIn && user?.username === auctionInfo.currentWinner ? (
                          <span className="current-winner-self">🎯 你是目前最高出價</span>
                        ) : (
                          auctionInfo.currentWinner
                        )}
                      </span>
                    </div>
                  )}
                </div>

                {!isAuctionEnded() ? (
                  <div className="bid-section">
                    <div className="bid-input-group">
                      <input
                        type="number"
                        className="bid-input"
                        value={bidAmount}
                        min={Number(auctionInfo?.currentPrice || 0) + 10}
                        step="10"
                        onChange={e => setBidAmount(e.target.value)}
                        placeholder={`最少 NT$ ${Number(auctionInfo?.currentPrice || 0) + 10}`}
                      />
                      <button
                        className="bid-btn"
                        onClick={handleBid}
                        disabled={!bidAmount || Number(bidAmount) <= (auctionInfo?.currentPrice || 0)}
                      >
                        出價
                      </button>
                    </div>
                    {product.currentPrice && (
                      <button
                        className="buy-now-btn"
                        onClick={handleBuyNow}
                      >
                        直購 NT$ {Number(product.currentPrice).toLocaleString()}
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="auction-ended">
                    {isLoggedIn && user?.username === auctionInfo?.currentWinner ? (
                      <div className="winner-message">
                        🎉 <span className="winner-text">恭喜你得標！</span>
                      </div>
                    ) : (
                      <div className="auction-ended-message">
                        競標已結束
                        {auctionInfo?.currentWinner && (
                          <div className="final-winner">得標者：{auctionInfo.currentWinner}</div>
                        )}
                      </div>
                    )}
                  </div>
                )}

                <div className="recent-bids">
                  <h4>最近出價</h4>
                  {recentBids.length > 0 ? (
                    <ul className="bid-history">
                      {recentBids.slice(0, 5).map((bid, idx) => (
                        <li key={idx} className="bid-record">
                          <span className="bid-amount">NT$ {Number(bid.amount).toLocaleString()}</span>
                          <span className="bid-user">
                            {isLoggedIn && user?.username === (bid.bidderName || bid.bidder_id) ? (
                              <span className="bid-user-self">你</span>
                            ) : (
                              bid.bidderName || bid.bidder_id
                            )}
                          </span>
                          <span className="bid-time">{new Date(bid.bidTime).toLocaleString()}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="no-bids">暫無出價紀錄</p>
                  )}
                </div>
              </div>
            </div>
          )}

          {!isAuctionProduct() && specs.length > 0 && product.skus && product.skus.length > 0 && (
            <div className="specs-selection">
              <h3 className="specs-title">規格</h3>
              {specs.map((spec, idx) => (
                <div key={idx} className="spec-row">
                  <span className="spec-name">{spec.name}</span>
                  <div className="spec-options-list">
                    {spec.options.map((opt, optIdx) => {
                      const isSelected = isOptionSelected(spec.name, opt);
                      const isOutOfStock = isOptionOutOfStock(spec.name, opt);
                      return (
                        <button
                          key={optIdx}
                          className={`spec-option-btn${isSelected ? ' selected' : ''}${isOutOfStock ? ' disabled' : ''}`}
                          onClick={() => !isOutOfStock && handleSpecChange(spec.name, opt)}
                          disabled={isOutOfStock}
                          title={isOutOfStock ? '此規格已售完' : undefined}
                        >
                          {opt}
                          {isOutOfStock && <span className="sold-out-tag">售完</span>}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}

          {!isAuctionProduct() && (
            <div className="quantity-section">
              <span className="quantity-label">數量</span>
              <div className="quantity-controls">
                <button
                  className="qty-btn decrease"
                  onClick={() => setQuantity(q => Math.max(1, q - 1))}
                  disabled={quantity <= 1}
                >−</button>
                <input
                  type="number"
                  className="qty-input"
                  min="1"
                  max={stock}
                  value={quantity}
                  onChange={e => handleQuantityChange(e.target.value)}
                />
                <button
                  className="qty-btn increase"
                  onClick={() => setQuantity(q => Math.min(stock, q + 1))}
                  disabled={quantity >= stock}
                >+</button>
              </div>
              <div className="stock-hint">(剩餘 {stock} 件)</div>
            </div>
          )}

          <div className="description-section">
            <h3>商品描述</h3>
            <div className="description-content">
              {product.description || "暫無商品描述"}
            </div>
          </div>

          <div className="action-buttons">
             {isAuctionProduct() ? (
               isSeller() ? (
           <div className="seller-hint">
          <span className="seller-badge">這是你上架的商品，僅供瀏覽，無法競標或直購。</span>
           </div>
              ) : (
              isAuctionEnded() ? (
                isLoggedIn && user?.username === auctionInfo?.currentWinner ? (
                 <button
  className="action-btn winner-pay-btn"
  onClick={() => {
    navigate('/checkout', {
      state: {
        items: [
          {
            productId: product.id,
            name: product.name,
            price: auctionInfo.currentPrice,
            quantity: 1,
            imageUrl: product.mainImageUrl,
            skuId: null 
          }
        ]
      }
    });
  }}
>
  💳 前往付款
</button>
                ) : (
                  <button className="action-btn disabled-btn" disabled>
                    競標已結束
                  </button>
                )
              ) : (
                <>
                  <button
                    className="action-btn bid-action-btn"
                    onClick={() => {
                      const bidInput = document.querySelector('.bid-input');
                      if (bidInput) bidInput.focus();
                    }}
                  >
                    🔨 立即出價
                  </button>
                  {product.currentPrice && (
                    <button
                      className="action-btn buy-now-action-btn"
                      onClick={handleBuyNow}
                    >
                      💳 直購 NT$ {Number(product.currentPrice).toLocaleString()}
                    </button>
                  )}
                </>
              )
            )
             ) : 
             isSeller() ? (
      <div className="seller-hint">
        <span className="seller-badge">
          這是你上架的商品，僅供瀏覽，無法加入購物車或購買。
        </span>
      </div>
            
            ) : (
            
              <>
                <button
                  className="action-btn cart-btn"
                  disabled={stock <= 0}
                  onClick={handleAddToCart}
                >
                  🛒 加入購物車
                </button>
                <button
                  className="action-btn buy-btn"
                  disabled={stock <= 0}
                  onClick={handleBuyNow}
                >
                  💳 {stock > 0 ? "立即購買" : "已售完"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
      <Footer />
    </div>
  );
}
