import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";
import "./CheckoutPage.css";
import { useCart } from "../contexts/CartContext";
import { useLocation } from "react-router-dom";


const API_BASE_URL = 'http://localhost:8080';

function getImageUrl(imageUrl) {
  if (!imageUrl) return "/default-product.jpg";
  if (imageUrl.startsWith('http')) return imageUrl;
  if (imageUrl.startsWith('/')) return API_BASE_URL + imageUrl;
  return API_BASE_URL + '/uploads/' + imageUrl;
}


function ProductImage({ item }) {
 const [imgSrc, setImgSrc] = useState(() => {
    return getImageUrl(item.imageUrl);
  });
  const [imageError, setImageError] = useState(false);

  const handleImageError = () => {
    console.log('圖片載入失敗:', imgSrc);
    if (!imageError && !imgSrc.includes('/default-product.jpg')) {
      setImgSrc('/default-product.jpg');
      setImageError(true);
    }
  };

 
  React.useEffect(() => {
    console.log('ProductImage - 當前商品:', {
      productName: item.productName || item.name,
      imageUrl: item.imageUrl,
      main_image_url: item.main_image_url,
      productImage: item.productImage,
      processedImgSrc: imgSrc
    });
  }, [item, imgSrc]);

  return (
    <div className="checkout-item-image">
      <img
        src={imgSrc}
        alt={item.productName || item.name}
        onError={handleImageError}
        onLoad={() => console.log('✅ 圖片載入成功:', imgSrc)}
        loading="lazy"
      />
    </div>
  );
}

export default function CheckoutPage() {
  const { cartItems, clearCart } = useCart();
  const navigate = useNavigate();
  
  const [shippingInfo, setShippingInfo] = useState({
    name: "",
    phone: "",
    address: "",
    note: "",
  });
  
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
   const location = useLocation();
  const { items } = location.state || {};


  const orderItems = items || cartItems;

  
  React.useEffect(() => {
    console.log('CheckoutPage cartItems:', cartItems);
    cartItems.forEach((item, index) => {
      console.log(`Cart Item ${index}:`, {
        cartItemId: item.cartItemId,
        productName: item.productName || item.name,
        imageUrl: item.imageUrl,
        main_image_url: item.main_image_url,
        productImage: item.productImage,
        price: item.price || item.current_price
      });
    });
  }, [cartItems]);


  const totalAmount = orderItems.reduce((sum, item) => {
  const price = Number(item.price || item.current_price || 0);
  const quantity = Number(item.quantity || 1);
  return sum + (price * quantity);
}, 0);


  const handleChange = e => {
    setShippingInfo({ 
      ...shippingInfo, 
      [e.target.name]: e.target.value 
    });
  };

 
  const handleBack = () => {
    navigate(-1);
  };

 const handleSubmit = async e => {
  e.preventDefault();
  setError("");
  setSubmitting(true);


  if (!shippingInfo.name || !shippingInfo.phone || !shippingInfo.address) {
    setError("請填寫完整收件人資訊");
    setSubmitting(false);
    return;
  }

  if (orderItems.length === 0) {
  setError("沒有商品可結帳");
  setSubmitting(false);
  return;
}

  try {
    const orderData = {
     orderItems: orderItems.map(item => ({
  productId: item.productId || item.id,
  skuId: item.skuId,
  quantity: item.quantity,
  price: item.price || item.current_price
})),
      totalAmount: totalAmount,
      shippingInfo: {
        name: shippingInfo.name,
        phone: shippingInfo.phone,
        address: shippingInfo.address,
        note: shippingInfo.note
      }
    };

    console.log("📤 提交訂單資料:", orderData);

    const response = await fetch('http://localhost:8080/api/orders', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(orderData)
    });

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (jsonError) {
        console.warn("無法解析錯誤回應為JSON:", jsonError);
      }
      throw new Error(errorMessage);
    }

    const result = await response.json();
    console.log("✅ 訂單創建回應:", result);

    if (result.success) {
     
      if (clearCart) {
        clearCart();
        console.log("🗑️ 前端購物車已清空");
      }
      
      alert(`訂單創建成功！訂單編號：${result.orderNumber}`);
      navigate("/profile", { state: { tab: "orders" } });
    } else {
      throw new Error(result.message || "訂單創建失敗");
    }
    
  } catch (err) {
    console.error("❌ 提交訂單失敗:", err);
    setError(err.message || "網路錯誤，請稍後再試");
  } finally {
    setSubmitting(false);
  }
};


  if (!orderItems || orderItems.length === 0) {
  return (
    <div className="checkout-bg">
      <Header />
      <main className="checkout-main" style={{ maxWidth: 600, margin: "0 auto" }}>
        <div className="checkout-container">
          <div className="checkout-header">
            <h2 className="checkout-title">結帳</h2>
            <button onClick={handleBack} className="btn-back">
              ← 返回
            </button>
          </div>
          <div className="checkout-empty">
            <div className="empty-cart-icon">🛒</div>
            <p>沒有商品可結帳！</p>
            <button onClick={() => navigate("/")} className="checkout-submit-btn">
              回到首頁
            </button>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}

  return (
    <div className="checkout-bg">
      <Header />
      <main className="checkout-main" style={{ maxWidth: 600, margin: "0 auto" }}>
        <div className="checkout-container">
          
         
          <div className="checkout-header">
            <h2 className="checkout-title">結帳</h2>
            <button onClick={handleBack} className="btn-back">
              ← 返回
            </button>
          </div>

          <form className="checkout-form" onSubmit={handleSubmit}>
            
      
            <section className="checkout-section">
              <h4>📦 購物清單 ({orderItems.length} 件商品)</h4>
              <ul className="checkout-cart-list">
                {orderItems.map(item => (
                  <li key={item.cartItemId || item.id} className="checkout-cart-item">
                    
                 
                    <ProductImage item={item} />
                    
             
                    <div className="checkout-item-details">
                      <div className="checkout-item-info">
                        <span className="checkout-item-name">
                          {item.productName || item.name}
                        </span>
                        {item.skuSpec && (
                          <span className="checkout-item-spec">
                            規格: {item.skuSpec}
                          </span>
                        )}
                        <span className="checkout-item-qty">
                          數量: {item.quantity}
                        </span>
                      </div>
                      
                  
                      <div className="checkout-item-price">
                        <span className="item-unit-price">
                          單價: NT$ {Number(item.price || item.current_price || 0).toLocaleString()}
                        </span>
                        <span className="item-total-price">
                          小計: NT$ {(Number(item.price || item.current_price || 0) * Number(item.quantity || 1)).toLocaleString()}
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
              
         
              <div className="checkout-total">
                <span>總金額：</span>
                <span className="total-amount">
                  NT$ {totalAmount.toLocaleString()}
                </span>
              </div>
            </section>

            <section className="checkout-section">
              <h4>📍 收件人資訊</h4>
              <div className="checkout-form-fields">
                <label>
                  姓名 <span className="required">*</span>
                  <input 
                    name="name" 
                    type="text"
                    value={shippingInfo.name} 
                    onChange={handleChange}
                    placeholder="請輸入收件人姓名"
                    required 
                  />
                </label>
                <label>
                  電話 <span className="required">*</span>
                  <input 
                    name="phone" 
                    type="tel"
                    value={shippingInfo.phone} 
                    onChange={handleChange}
                    placeholder="請輸入聯絡電話"
                    required 
                  />
                </label>
                <label>
                  地址 <span className="required">*</span>
                  <input 
                    name="address" 
                    type="text"
                    value={shippingInfo.address} 
                    onChange={handleChange}
                    placeholder="請輸入完整收件地址"
                    required 
                  />
                </label>
                <label>
                  備註
                  <textarea 
                    name="note" 
                    value={shippingInfo.note} 
                    onChange={handleChange}
                    placeholder="如有特殊需求請告知..."
                    rows="3"
                  />
                </label>
              </div>
            </section>

       
            {error && (
              <div className="checkout-error">
                <span className="error-icon">⚠️</span>
                {error}
              </div>
            )}

          
            <div className="checkout-actions">
              <button 
                type="button"
                onClick={handleBack}
                className="btn-secondary"
              >
                返回購物車
              </button>
              <button 
                type="submit" 
                className="btn-primary" 
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <span className="loading-spinner"></span>
                    處理中...
                  </>
                ) : (
                  <>
                    💳 確認訂單
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </main>
      <Footer />
    </div>
  );
}
