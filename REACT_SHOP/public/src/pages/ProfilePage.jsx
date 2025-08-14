import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../utils/api";
import { useAuth } from "../contexts/AuthContext";
import Header from "../components/Header";
import SafeImage from "../components/SafeImage";
import "./ProfilePage.css";

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", email: "" });
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [pwdForm, setPwdForm] = useState({ oldPassword: "", newPassword: "" });
  const [pwdMsg, setPwdMsg] = useState("");
  const [activeTab, setActiveTab] = useState("profile");
  const [favorites, setFavorites] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) setForm({ username: user.username, email: user.email });
  }, [user]);

  const loadFavorites = async () => {
    try {
    setLoading(true);
    console.log('🔍 開始載入收藏...');
    
    const response = await api.get("/api/favorites/my-favorites");
    console.log('📡 收藏 API 回傳:', response.data);
    
    if (response.data.success) {
      const favoritesData = response.data.favorites || [];
      console.log('📋 收藏資料:', favoritesData);
      console.log('📊 收藏數量:', favoritesData.length);
      
      setFavorites(favoritesData);
      
      // 檢查每個收藏項目的圖片
      favoritesData.forEach((item, index) => {
        console.log(`📸 收藏 ${index + 1} 圖片:`, item.productImage);
      });
    } else {
      console.error('❌ 收藏 API 回傳失敗:', response.data);
    }
  } catch (error) {
    console.error("❌ 載入收藏失敗:", error);
    console.error("❌ 錯誤詳情:", error.response?.data);
    setMessage("載入收藏列表失敗");
  } finally {
    setLoading(false);
  }
};

  const loadOrders = async () => {
    try {
      setLoading(true);
      const response = await api.get("/api/orders/my-orders");
      if (response.data.success) setOrders(response.data.orders || []);
    } catch (error) {
      console.error("載入購買紀錄失敗:", error);
      setMessage("載入購買紀錄失敗");
    } finally {
      setLoading(false);
    }
  };

  const removeFavorite = async (productId) => {
    try {
      const response = await api.delete(`/api/favorites/${productId}`);
      if (response.data.success) {
        setFavorites(favorites.filter(item => item.productId !== productId));
        setMessage("已移除收藏");
        setTimeout(() => setMessage(""), 2000);
      }
    } catch (error) {
      console.error("移除收藏失敗:", error);
      setMessage("移除收藏失敗");
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setMessage("");
    if (tab === "favorites" && favorites.length === 0) loadFavorites();
    else if (tab === "orders" && orders.length === 0) loadOrders();
  };

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSave = async () => {
    try {
      const res = await api.put("/api/users/me", { username: form.username });
      setUser({ ...user, username: res.data.username });
      setEditing(false);
      setMessage("資料已更新");
      setTimeout(() => setMessage(""), 3000);
    } catch (err) {
      setMessage("更新失敗，請稍後再試");
      setTimeout(() => setMessage(""), 3000);
    }
  };

  const handleClear = () => {
    setForm({ username: "", email: user?.email || "" });
    setMessage("欄位已清空");
    setTimeout(() => setMessage(""), 2000);
  };

  const handleCancel = () => {
    setForm({ username: user?.username || "", email: user?.email || "" });
    setEditing(false);
    setMessage("");
  };

  const handleBackToHome = () => navigate("/");

  const handlePwdChange = async () => {
    setPwdMsg("");
    if (!pwdForm.oldPassword || !pwdForm.newPassword) {
      setPwdMsg("請輸入完整欄位");
      return;
    }
    if (pwdForm.newPassword.length < 6) {
      setPwdMsg("新密碼至少需要6個字元");
      return;
    }
    try {
      await api.put("/api/users/me/password", pwdForm);
      setPwdMsg("密碼已更新");
      setPwdForm({ oldPassword: "", newPassword: "" });
      setShowPwd(false);
      setTimeout(() => setPwdMsg(""), 3000);
    } catch (err) {
      setPwdMsg("密碼更新失敗，請確認舊密碼正確");
      setTimeout(() => setPwdMsg(""), 3000);
    }
  };

  const handleTogglePwd = () => {
    setShowPwd(v => !v);
    setPwdMsg("");
    setPwdForm({ oldPassword: "", newPassword: "" });
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleCancelOrder = async (orderId) => {
  if (!window.confirm('確定要取消此訂單嗎？')) return;
  try {
    setLoading(true);
    const response = await api.put(`/api/orders/${orderId}/cancel`);
    if (response.data.success) {
      setMessage("訂單已取消");
      await loadOrders();
      setTimeout(() => setMessage(""), 3000);
    } else {
      setMessage(response.data.message || "取消訂單失敗");
      setTimeout(() => setMessage(""), 3000);
    }
  } catch (error) {
    setMessage(error.response?.data?.message || "取消訂單失敗，請稍後再試");
    setTimeout(() => setMessage(""), 3000);
  } finally {
    setLoading(false);
  }
};


  const getOrderStatusText = (status) => {
    const statusMap = {
      'PENDING': '待處理',
      'CONFIRMED': '已確認',
      'PROCESSING': '處理中',
      'SHIPPED': '已出貨',
      'DELIVERED': '已送達',
      'CANCELLED': '已取消',
      'REFUNDED': '已退款'
    };
    return statusMap[status] || status;
  };
 
  return (
    <>
      <Header />
      <div className="profile-container">
        <div className="profile-tabs">
          <button className={`tab-btn ${activeTab === 'profile' ? 'active' : ''}`} onClick={() => handleTabChange('profile')}>👤 個人資料</button>
          <button className={`tab-btn ${activeTab === 'favorites' ? 'active' : ''}`} onClick={() => handleTabChange('favorites')}>❤️ 我的收藏</button>
          <button className={`tab-btn ${activeTab === 'orders' ? 'active' : ''}`} onClick={() => handleTabChange('orders')}>📦 購買紀錄</button>
        </div>

        {activeTab === 'profile' && (
          <div className="profile-card">
            <h2 className="profile-title">個人資料</h2>
            <div className="profile-row">
              <label className="profile-label">用戶名稱</label>
              {editing ? (
                <input className="profile-input" name="username" value={form.username} onChange={handleChange} placeholder="請輸入用戶名稱" />
              ) : (
                <span className="profile-value">{user?.username || "未設定"}</span>
              )}
            </div>
            <div className="profile-row">
              <label className="profile-label">Email</label>
              <span className="profile-value">{user?.email || "未設定"}</span>
            </div>
            <div className="profile-actions">
              {editing ? (
                <>
                  <button className="profile-btn" onClick={handleSave} type="button">儲存</button>
                  <button className="profile-btn-outline" onClick={handleClear} type="button">清空</button>
                  <button className="profile-btn-outline" onClick={handleCancel} type="button">取消</button>
                </>
              ) : (
                <>
                  <button className="profile-btn" onClick={() => { setEditing(true); setMessage(""); }} type="button">編輯</button>
                  <button className="profile-btn-outline" onClick={handleBackToHome} type="button">返回首頁</button>
                </>
              )}
            </div>
            <div className="profile-row password-section">
              <label className="profile-label">密碼</label>
              <button className="profile-btn-outline" onClick={handleTogglePwd} type="button">{showPwd ? "取消" : "修改密碼"}</button>
            </div>
            {showPwd && (
              <div className="profile-pwd-edit">
                <input className="profile-input" type="password" placeholder="請輸入舊密碼" value={pwdForm.oldPassword} onChange={e => setPwdForm(f => ({ ...f, oldPassword: e.target.value }))} />
                <input className="profile-input" type="password" placeholder="請輸入新密碼（至少6個字元）" value={pwdForm.newPassword} onChange={e => setPwdForm(f => ({ ...f, newPassword: e.target.value }))} />
                <button className="profile-btn" onClick={handlePwdChange} type="button">儲存密碼</button>
                {pwdMsg && <div className="profile-message">{pwdMsg}</div>}
              </div>
            )}
            {message && <div className="profile-message">{message}</div>}
          </div>
        )}

    {activeTab === 'favorites' && (
  <div className="profile-card">
    <h2 className="profile-title">我的收藏</h2>
    {loading ? (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>載入中...</p>
      </div>
    ) : favorites.length === 0 ? (
      <div className="empty-state">
        <div className="empty-icon">❤️</div>
        <h3>還沒有收藏商品</h3>
        <p>快去發現喜歡的商品吧！</p>
        <button className="profile-btn" onClick={() => navigate("/")}>開始購物</button>
      </div>
    ) : (
      <div className="favorites-list">
        {favorites.map(item => (
          <div key={item.favoriteId || item.productId} className="favorite-item-text">
            <div className="favorite-info">
              <h4 className="favorite-name" onClick={() => navigate(`/product/${item.productId}`)}>{item.productName}</h4>
              <p className="favorite-price">NT$ {Number(item.price || 0).toLocaleString()}</p>
              <div className="favorite-date">收藏於 {formatDate(item.createdAt)}</div>
            </div>
            <div className="favorite-actions">
              <button className="btn-add-cart" onClick={() => navigate(`/product/${item.productId}`)}>查看商品</button>
              <button className="btn-remove-favorite" onClick={() => removeFavorite(item.productId)}>移除收藏</button>
            </div>
          </div>
        ))}
      </div>
    )}
    <div className="profile-actions">
      <button className="profile-btn-outline" onClick={handleBackToHome} type="button">返回首頁</button>
    </div>
  </div>
)}

 {activeTab === 'orders' && (
  <div className="profile-card">
    <h2 className="profile-title">購買紀錄</h2>
    {loading ? (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>載入中...</p>
      </div>
    ) : orders.length === 0 ? (
      <div className="empty-state">
        <div className="empty-icon">📦</div>
        <h3>還沒有購買紀錄</h3>
        <p>快去選購心儀的商品吧！</p>
        <button className="profile-btn" onClick={() => navigate("/")}>開始購物</button>
      </div>
    ) : (
      <div className="orders-list">
        {orders.map(order => (
          <div key={order.orderId} className="order-item">
            <div className="order-header">
              <div className="order-info">
                <h4>訂單編號：ORD{String(order.orderId).padStart(8, '0')}</h4>
                <p className="order-date">{formatDate(order.createdAt)}</p>
              </div>
              <div className="order-status">
                <span className={`status-badge ${order.status.toLowerCase()}`}>{getOrderStatusText(order.status)}</span>
              </div>
              <div className="order-actions-horizontal">
                <button className="btn-view-detail" onClick={() => navigate(`/order-detail/${order.orderId}`)}>查看詳情</button>
                {order.status === 'PENDING' && (
                  <button className="btn-cancel-order" onClick={() => handleCancelOrder(order.orderId)}>取消訂單</button>
                )}
              </div>
            </div>
            <div className="order-products">
            {order.orderItems.map((item, idx) => (
  <div className="order-product-item" key={item.productId || idx}>
    <div className="order-product-info">
      <span className="order-product-name">{item.productName}</span>
      <span className="order-product-qty">數量：{item.quantity}</span>
      <span className="order-product-price">NT$ {Number(item.price).toLocaleString()}</span>
    </div>
  </div>
))}
            </div>
            <div className="order-summary-row">
              <span className="order-total-label">訂單總額：</span>
              <span className="order-total-amount">NT$ {Number(order.totalPrice).toLocaleString()}</span>
            </div>
          </div>
        ))}
      </div>
    )}
    <div className="profile-actions">
      <button className="profile-btn-outline" onClick={handleBackToHome} type="button">返回首頁</button>
    </div>
  </div>
)}


      </div>
    </>
  );
}
