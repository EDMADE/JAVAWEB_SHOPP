import React, { useState } from 'react';
import './LoginModal.css';

export default function Register({ visible, onClose, onSwitchToLogin }) {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [step, setStep] = useState('register');

  const validateForm = () => {
    if (!formData.username.trim()) {
      setMessage('請輸入使用者名稱');
      setMessageType('error');
      return false;
    }

    if (formData.username.length < 3) {
      setMessage('使用者名稱至少需要3個字元');
      setMessageType('error');
      return false;
    }

    if (!formData.email.trim()) {
      setMessage('請輸入Email');
      setMessageType('error');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setMessage('請輸入有效的Email格式');
      setMessageType('error');
      return false;
    }

    if (!formData.password) {
      setMessage('請輸入密碼');
      setMessageType('error');
      return false;
    }

    if (formData.password.length < 6) {
      setMessage('密碼至少需要6個字元');
      setMessageType('error');
      return false;
    }

    if (formData.password !== formData.confirmPassword) {
      setMessage('兩次密碼不一致');
      setMessageType('error');
      return false;
    }

    return true;
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    if (message) {
      setMessage('');
      setMessageType('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setMessageType('');
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: formData.username.trim(),
          email: formData.email.trim().toLowerCase(),
          password: formData.password,
        }),
      });
      
      const data = await res.json();
      
      if (res.ok) {
        setStep('verification-sent');
        setMessage('註冊成功！請檢查您的email並點擊驗證鏈接以啟用帳號。');
        setMessageType('success');
        
        console.log('✅ 註冊成功:', data);
      } else {
        setMessage(data.message || '註冊失敗，請稍後再試');
        setMessageType('error');
      }
    } catch (err) {
      console.error('註冊錯誤:', err);
      setMessage('網路錯誤，請檢查網路連接後再試');
      setMessageType('error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendVerification = async () => {
    setIsLoading(true);
    setMessage('');
    setMessageType('');
    
    try {
      const res = await fetch('http://localhost:8080/api/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formData.email }),
      });
      
      const data = await res.json();
      
      if (res.ok) {
        setMessage('驗證郵件已重新發送，請檢查您的收件匣和垃圾郵件匣。');
        setMessageType('success');
      } else {
        setMessage(data.message || '發送失敗，請稍後再試。');
        setMessageType('error');
      }
    } catch (err) {
      console.error('重發驗證郵件錯誤:', err);
      setMessage('網路錯誤，請稍後再試');
      setMessageType('error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setStep('register');
    setFormData({ username: '', email: '', password: '', confirmPassword: '' });
    setMessage('');
    setMessageType('');
    setIsLoading(false);
    onClose();
  };

  const handleSwitchToLogin = () => {
    handleClose();
    if (onSwitchToLogin) {
      onSwitchToLogin();
    }
  };

  if (!visible) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>會員註冊</h2>
          <button className="close-btn" onClick={handleClose} aria-label="關閉">×</button>
        </div>

        {step === 'register' ? (
          <form onSubmit={handleSubmit} className="login-form" noValidate>
            <div className="form-group">
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="使用者名稱"
                required
                disabled={isLoading}
                autoComplete="username"
                maxLength="50"
              />
            </div>
            
            <div className="form-group">
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="Email"
                required
                disabled={isLoading}
                autoComplete="email"
                maxLength="100"
              />
            </div>
            
            <div className="form-group">
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="密碼（至少6個字元）"
                required
                disabled={isLoading}
                autoComplete="new-password"
                minLength="6"
              />
            </div>
            
            <div className="form-group">
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="再次輸入密碼"
                required
                disabled={isLoading}
                autoComplete="new-password"
              />
            </div>
            
            {message && (
              <div className={`message ${messageType}`} role="alert">
                {message}
              </div>
            )}
            
            <button 
              type="submit" 
              disabled={isLoading} 
              className="submit-btn"
              aria-label={isLoading ? '註冊中' : '註冊'}
            >
              {isLoading ? '註冊中...' : '註冊'}
            </button>
            
            <div className="form-footer">
              已有帳號？
              <span 
                onClick={handleSwitchToLogin} 
                className="link-btn"
                tabIndex="0"
                role="button"
                onKeyDown={(e) => e.key === 'Enter' && handleSwitchToLogin()}
              >
                返回登入
              </span>
            </div>
          </form>
        ) : (
          <div className="verification-info">
            <div className="verification-icon">📧</div>
            <h3>請驗證您的Email</h3>
            <p>我們已發送驗證郵件至：</p>
            <div className="email-display">{formData.email}</div>
            
            {message && (
              <div className={`message ${messageType}`} role="alert">
                {message}
              </div>
            )}
            
            <div className="verification-actions">
              <button 
                onClick={handleResendVerification} 
                disabled={isLoading}
                className="resend-btn"
                aria-label={isLoading ? '發送中' : '重新發送驗證郵件'}
              >
                {isLoading ? '發送中...' : '重新發送驗證郵件'}
              </button>
              
              <button 
                onClick={handleClose} 
                className="close-btn-text"
                aria-label="關閉"
              >
                關閉
              </button>
            </div>
            
            <div className="verification-note">
              <small>
                沒收到郵件？請檢查垃圾郵件匣。
                <br/>
                驗證鏈接將在30分鐘後過期。
                <br/>
                如果仍有問題，請聯繫客服。
              </small>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
