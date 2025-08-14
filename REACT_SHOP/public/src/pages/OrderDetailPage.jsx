import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Header from '../components/Header';
import Footer from '../components/Footer';
import './OrderDetailPage.css';

const API_BASE_URL = 'http://localhost:8080';

function getImageUrl(imageUrl) {
  if (!imageUrl) return "/default-product.jpg";
  if (imageUrl.startsWith('http')) return imageUrl;
  if (imageUrl.startsWith('/')) return API_BASE_URL + imageUrl;
  return API_BASE_URL + '/uploads/' + imageUrl;
}

export default function OrderDetailPage() {
  const { id } = useParams();
  const { user, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(null);
  const [orderItems, setOrderItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/');
      return;
    }

    const fetchOrderDetail = async () => {
      try {
        setLoading(true);
        const response = await fetch(`${API_BASE_URL}/api/orders/${id}/detail`, {
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error('訂單不存在或無權限查看');
        }

        const data = await response.json();
        setOrder(data.order);
        setOrderItems(data.orderItems || []);
      } catch (err) {
        console.error('載入訂單詳情失敗:', err);
        setError(err.message || '載入失敗');
      } finally {
        setLoading(false);
      }
    };

    fetchOrderDetail();
  }, [id, isLoggedIn, navigate]);

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('zh-TW', {
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

  const getStatusColor = (status) => {
    const colorMap = {
      'PENDING': '#ffc107',
      'CONFIRMED': '#17a2b8',
      'PROCESSING': '#007bff',
      'SHIPPED': '#28a745',
      'DELIVERED': '#28a745',
      'CANCELLED': '#dc3545',
      'REFUNDED': '#6c757d'
    };
    return colorMap[status] || '#6c757d';
  };

  if (loading) {
    return (
      <div className="order-detail-container">
        <Header />
        <main className="order-detail-main">
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>載入中...</p>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="order-detail-container">
        <Header />
        <main className="order-detail-main">
          <div className="error-container">
            <h2>載入失敗</h2>
            <p>{error || '訂單不存在'}</p>
            <button onClick={() => navigate('/profile')} className="btn-primary">
              返回個人中心
            </button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="order-detail-container">
      <Header />
      <main className="order-detail-main">
        <div className="order-detail-content">
          
          <div className="order-detail-header">
            <button onClick={() => navigate('/profile')} className="btn-back">
              ← 返回個人中心
            </button>
            <h1>訂單詳情</h1>
          </div>

          <div className="order-info-card">
            <div className="order-info-header">
              <div className="order-number">
                <h2>訂單編號：ORD{String(order.orderId).padStart(8, '0')}</h2>
                <span 
                  className="order-status"
                  style={{ backgroundColor: getStatusColor(order.status) }}
                >
                  {getOrderStatusText(order.status)}
                </span>
              </div>
              <div className="order-date">
                下單時間：{formatDate(order.createdAt)}
              </div>
            </div>

            <div className="order-info-details">
              <div className="shipping-info">
                <h3>📍 收件資訊</h3>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">收件人：</span>
                    <span className="info-value">{order.receiverName}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">聯絡電話：</span>
                    <span className="info-value">{order.receiverPhone}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">收件地址：</span>
                    <span className="info-value">{order.receiverAddress}</span>
                  </div>
                  {order.note && (
                    <div className="info-item">
                      <span className="info-label">備註：</span>
                      <span className="info-value">{order.note}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="order-items-card">
            <h3>📦 訂單商品</h3>
            <div className="order-items-list">
              {orderItems.map((item, index) => (
                <div key={item.itemId || index} className="order-item">
                  <div className="item-image">
                    <img 
                      src={getImageUrl(item.productImage)} 
                      alt={item.productName}
                      onError={(e) => { e.target.src = '/default-product.jpg'; }}
                    />
                  </div>
                  <div className="item-details">
                    <h4 className="item-name">{item.productName}</h4>
                    {item.skuSpec && (
                      <p className="item-spec">規格：{item.skuSpec}</p>
                    )}
                    <div className="item-quantity-price">
                      <span className="item-quantity">數量：{item.quantity}</span>
                      <span className="item-price">
                        NT$ {Number(item.price).toLocaleString()}
                      </span>
                    </div>
                  </div>
                  <div className="item-total">
                    NT$ {(Number(item.price) * Number(item.quantity)).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="order-summary-card">
            <h3>💰 費用摘要</h3>
            <div className="summary-details">
              <div className="summary-row">
                <span>商品小計：</span>
                <span>NT$ {Number(order.totalPrice).toLocaleString()}</span>
              </div>
              <div className="summary-row">
                <span>運費：</span>
                <span>NT$ 80</span>
              </div>
              <div className="summary-row total-row">
                <span>訂單總額：</span>
                <span className="total-amount">NT$ {Number(order.totalPrice).toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="order-actions">
            {order.status === 'PENDING' && (
              <button className="btn-cancel" onClick={() => {
                if (confirm('確定要取消此訂單嗎？')) {
                }
              }}>
                取消訂單
              </button>
            )}
            <button className="btn-primary" onClick={() => navigate('/profile')}>
              返回訂單列表
            </button>
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
}
