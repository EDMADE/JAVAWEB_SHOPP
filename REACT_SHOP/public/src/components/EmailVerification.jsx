import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

export default function EmailVerification() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('');
  const [hasVerified, setHasVerified] = useState(false);
useEffect(() => {
  if (token && !hasVerified) {
    setHasVerified(true);
    verifyEmail(token);
  }
}, [token, hasVerified]);


  const verifyEmail = async (verificationToken) => {
    try {
      setStatus('verifying');
      setMessage('正在驗證您的Email，請稍候...');
      
      console.log('🔍 開始驗證token:', verificationToken);
      
      const res = await fetch(`http://localhost:8080/api/auth/verify-email/${verificationToken}`, {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      console.log('📡 API回應狀態:', res.status);
      
      const data = await res.json();
      console.log('📋 API回應數據:', data);
      
     
      if (res.ok && (data.verified === true || data.success === true)) {
        setStatus('success');
        setMessage(data.message || 'Email驗證成功！您現在可以登入了。');
        
        console.log('✅ Email驗證成功');
        
       
        setTimeout(() => {
          navigate('/');
        }, 3000);
      } else {
        setStatus('error');
        
        if (res.status === 400) {
          setMessage(data.message || '驗證鏈接無效或已過期，請重新註冊或聯繫客服');
        } else if (res.status === 500) {
          setMessage('系統暫時無法處理您的請求，請稍後再試');
        } else {
          setMessage(data.message || '驗證失敗，請檢查驗證鏈接或聯繫客服');
        }
        
        console.error('❌ Email驗證失敗:', data);
      }
    } catch (error) {
      console.error('❌ 驗證過程發生錯誤:', error);
      setStatus('error');
      setMessage('網路連接錯誤，請檢查網路後再試，或聯繫客服協助');
    }
  };

 
  const handleResendVerification = () => {
    navigate('/');
  };

  return (
    <div className="email-verification-page" style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      backgroundColor: '#f5f5f5',
      fontFamily: 'Arial, sans-serif'
    }}>
      <div className="verification-container" style={{
        backgroundColor: 'white',
        padding: '40px',
        borderRadius: '12px',
        boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
        textAlign: 'center',
        maxWidth: '500px',
        margin: '20px'
      }}>
        {status === 'verifying' && (
          <>
            <div className="loading-spinner" style={{
              border: '4px solid #f3f3f3',
              borderTop: '4px solid #007bff',
              borderRadius: '50%',
              width: '50px',
              height: '50px',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 20px'
            }}></div>
            <h2 style={{ color: '#333', marginBottom: '10px' }}>正在驗證...</h2>
            <p style={{ color: '#666' }}>{message}</p>
          </>
        )}
        
        {status === 'success' && (
          <>
            <div className="success-icon" style={{ 
              fontSize: '64px', 
              color: '#28a745', 
              marginBottom: '20px',
              animation: 'bounce 0.6s ease-in-out'
            }}>
              ✅
            </div>
            <h2 style={{ color: '#28a745', marginBottom: '15px' }}>驗證成功！</h2>
            <p style={{ color: '#333', marginBottom: '10px' }}>{message}</p>
            <p style={{ color: '#666', fontSize: '14px' }}>即將自動跳轉到首頁...</p>
            
            <button 
              onClick={() => navigate('/')} 
              style={{
                background: '#28a745',
                color: 'white',
                border: 'none',
                padding: '12px 24px',
                borderRadius: '6px',
                cursor: 'pointer',
                marginTop: '20px',
                fontSize: '16px'
              }}
            >
              立即前往首頁
            </button>
          </>
        )}
        
        {status === 'error' && (
          <>
            <div className="error-icon" style={{ 
              fontSize: '64px', 
              color: '#dc3545', 
              marginBottom: '20px' 
            }}>
              ❌
            </div>
            <h2 style={{ color: '#dc3545', marginBottom: '15px' }}>驗證失敗</h2>
            <p style={{ color: '#333', marginBottom: '20px', lineHeight: '1.6' }}>
              {message}
            </p>
            
            <div style={{ marginTop: '20px' }}>
              <button 
                onClick={handleResendVerification}
                style={{
                  background: '#007bff',
                  color: 'white',
                  border: 'none',
                  padding: '12px 24px',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  marginRight: '10px',
                  fontSize: '16px'
                }}
              >
                返回首頁
              </button>
              
              <button 
                onClick={() => window.location.href = 'mailto:support@競好購.com'}
                style={{
                  background: '#6c757d',
                  color: 'white',
                  border: 'none',
                  padding: '12px 24px',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '16px'
                }}
              >
                聯繫客服
              </button>
            </div>
          </>
        )}
      </div>
      
      
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        @keyframes bounce {
          0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
          40% { transform: translateY(-10px); }
          60% { transform: translateY(-5px); }
        }
      `}</style>
    </div>
  );
}
