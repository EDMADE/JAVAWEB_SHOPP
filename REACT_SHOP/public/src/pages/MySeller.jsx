import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import api from "../utils/api";
import Header from "../components/Header";
import Footer from "../components/Footer";
import SafeImage from "../components/SafeImage";
import "./MySeller.css";

const API_BASE_URL = "http://localhost:8080";

export default function MySeller({ onCartClick, cartCount = 0 }) {
  const { user, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("products");
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!isLoggedIn) {
      navigate("/");
      return;
    }
  }, [isLoggedIn, navigate]);

  const fetchProducts = useCallback(async () => {
    if (!isLoggedIn || !user?.userId) {
      setLoading(false);
      return;
    }
    
    try {
      const res = await api.get(`/api/products/my?userId=${user.userId}`);
      setProducts(res.data);
    } catch (error) {
      console.error("載入商品失敗:", error);
      setProducts([]);
      setMessage("載入商品失敗");
    } finally {
      setLoading(false);
    }
  }, [isLoggedIn, user?.userId]);

  const fetchSellerOrders = useCallback(async () => {
    if (!isLoggedIn || !user?.userId) return;
    
    try {
      setLoading(true);
      const res = await api.get(`/api/orders/seller-orders?sellerId=${user.userId}`);
      if (res.data.success) {
        setOrders(res.data.orders || []);
      }
    } catch (error) {
      console.error("載入訂單失敗:", error);
      setOrders([]);
      setMessage("載入訂單失敗");
    } finally {
      setLoading(false);
    }
  }, [isLoggedIn, user?.userId]);

  useEffect(() => {
    if (activeTab === "products") {
      fetchProducts();
    } else if (activeTab === "orders") {
      fetchSellerOrders();
    }
  }, [activeTab, fetchProducts, fetchSellerOrders]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setMessage("");
  };

  const handleDeleteProduct = async (id) => {
    if (!window.confirm("確定要刪除這個商品嗎？")) return;
    try {
      await api.delete(`/api/products/${id}?userId=${user.userId}`);
      setProducts(products.filter(p => (p.id || p.product_id) !== id));
      setMessage("商品刪除成功！");
      setTimeout(() => setMessage(""), 3000);
    } catch (error) {
      console.error("刪除失敗:", error);
      setMessage("刪除失敗，請稍後再試");
    }
  };

  const handleUpdateOrderStatus = async (orderId, newStatus) => {
    try {
      const res = await api.put(`/api/orders/${orderId}/status`, { status: newStatus });
      if (res.data.success) {
        setOrders(orders.map(order => 
          order.orderId === orderId 
            ? { ...order, status: newStatus }
            : order
        ));
        setMessage("訂單狀態更新成功");
        setTimeout(() => setMessage(""), 3000);
      }
    } catch (error) {
      console.error("更新訂單狀態失敗:", error);
      setMessage("更新訂單狀態失敗");
    }
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

  if (!isLoggedIn) return null;

  return (
    <div className="my-seller-bg">
      <Header onCartClick={onCartClick} cartCount={cartCount} />
      <main className="my-seller-main">
        <h2 className="my-seller-title">我的賣場</h2>
        
        <div className="my-seller-nav">
          <button className="my-seller-add-btn" onClick={() => navigate("/")}>返回首頁</button>
          <button className="my-seller-add-btn" onClick={() => navigate("/add-product")}>＋ 新增商品</button>
        </div>

        <div className="my-seller-tabs">
          <button 
            className={`tab-btn ${activeTab === 'products' ? 'active' : ''}`}
            onClick={() => handleTabChange('products')}
          >
            📦 我的商品
          </button>
          <button 
            className={`tab-btn ${activeTab === 'orders' ? 'active' : ''}`}
            onClick={() => handleTabChange('orders')}
          >
            📋 訂單管理
          </button>
        </div>

        {message && <div className="my-seller-message">{message}</div>}
        {activeTab === 'products' && (
          <div className="my-seller-table-wrapper">
            {loading ? (
              <div className="loading-container">載入中...</div>
            ) : (
              <table className="my-seller-table">
                <thead>
                  <tr>
                    <th>圖片</th>
                    <th>名稱</th>
                    <th>價格</th>
                    <th>分類</th>
                    <th>商品狀況</th>
                    <th>庫存</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {products.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="my-seller-empty">
                        <div style={{fontSize: 48, marginBottom: 16}}>📦</div>
                        <div>尚未上架任何商品</div>
                        <button className="my-seller-add-btn" onClick={() => navigate("/add-product")}>
                          ＋ 新增商品
                        </button>
                      </td>
                    </tr>
                  ) : (
                    products.map(product => (
                      <tr key={product.id || product.product_id}>
                        <td>
                          <div className="my-seller-img-box">
                            <SafeImage
                              src={
                                product.productImages && product.productImages.length > 0
                                  ? product.productImages[0].imageUrl
                                  : product.mainImageUrl
                              }
                              alt={product.name}
                              className="my-seller-img"
                            />
                          </div>
                        </td>
                        <td>{product.name}</td>
                        <td>
                          {(() => {
                            const min = product.minPrice || product.currentPrice || 0;
                            const max = product.maxPrice || min;
                            return min !== max ? `NT$ ${min} ~ ${max}` : `NT$ ${min}`;
                          })()}
                        </td>
                        <td>{product.category}</td>
                        <td>{product.productCondition || "-"}</td>
                        <td>
                          {product.skus && product.skus.length > 0
                            ? product.skus.reduce((sum, sku) => sum + Number(sku.stock), 0)
                            : product.stockQuantity}
                        </td>
                        <td>
                          <button
                            className="my-seller-status-btn"
                            style={{
                              background: product.status === "ACTIVE" ? "#4caf50" : "#bbb"
                            }}
                            onClick={async () => {
                              const newStatus = product.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
                              try {
                                await api.patch(`/api/products/${product.id}/status`, { status: newStatus });
                                setProducts(products => 
                                  products.map(p => 
                                    p.id === product.id ? { ...p, status: newStatus } : p
                                  )
                                );
                              } catch (err) {
                                setMessage("狀態切換失敗，請稍後再試");
                              }
                            }}
                          >
                            {product.status === "ACTIVE" ? "下架" : "上架"}
                          </button>
                          <button 
                            className="my-seller-edit-btn"
                            onClick={() => navigate(`/edit-product/${product.id || product.product_id}`)}
                          >
                            編輯
                          </button>
                          <button 
                            className="my-seller-del-btn" 
                            onClick={() => handleDeleteProduct(product.id || product.product_id)}
                          >
                            刪除
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            )}
          </div>
        )}

        {activeTab === 'orders' && (
          <div className="my-seller-orders">
            {loading ? (
              <div className="loading-container">載入中...</div>
            ) : orders.length === 0 ? (
              <div className="my-seller-empty">
                <div style={{fontSize: 48, marginBottom: 16}}>📋</div>
                <div>暫無收到訂單</div>
              </div>
            ) : (
              <div className="orders-list">
                {orders.map(order => (
                  <div key={order.orderId} className="order-card">
                    <div className="order-header">
                      <div className="order-info">
                        <h4>訂單編號：ORD{String(order.orderId).padStart(8, '0')}</h4>
                        <p>下單時間：{formatDate(order.createdAt)}</p>
                        <p>買家：{order.buyerName}</p>
                      </div>
                      <div className="order-status">
                        <select
                          value={order.status}
                          onChange={(e) => handleUpdateOrderStatus(order.orderId, e.target.value)}
                          className="status-select"
                        >
                          <option value="PENDING">待處理</option>
                          <option value="CONFIRMED">已確認</option>
                          <option value="PROCESSING">處理中</option>
                          <option value="SHIPPED">已出貨</option>
                          <option value="DELIVERED">已送達</option>
                        </select>
                      </div>
                    </div>
                    <div className="order-products">
                     {order.orderItems.map((item, idx) => (
  <div key={idx} className="order-product-item">
    <div className="order-product-info">
      <span className="order-product-name">{item.productName}</span>
      <span className="order-product-qty">數量：{item.quantity}</span>
      <span className="order-product-price">NT$ {Number(item.price).toLocaleString()}</span>
    </div>
  </div>
))}
                    </div>
                    <div className="order-summary">
                      <span className="order-total">
                        訂單總額：NT$ {Number(order.totalPrice).toLocaleString()}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
