import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext'; 
import './LoginModal.css';

export default function LoginModal({ visible, onClose, defaultTab = 'login' }) {
  const { login } = useAuth(); 
  const [isLogin, setIsLogin] = useState(defaultTab === 'login');

  
  useEffect(() => {
    setIsLogin(defaultTab === 'login');
    setLoginMsg('');
  }, [defaultTab, visible]);

  // 登入表單狀態
  const [loginData, setLoginData] = useState({ username: '', password: '' });
  const [loginMsg, setLoginMsg] = useState('');
  const [regData, setRegData] = useState({ username: '', email: '', password: '', confirmPassword: '' });
  const [regMsg, setRegMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (visible) {
      setIsLogin(defaultTab === 'login');
      // 清空錯誤訊息
      setLoginMsg('');
      setRegMsg('');
    }
  }, [defaultTab, visible]);

 
  useEffect(() => {
    if (!visible) {
      setLoginData({ username: '', password: '' });
      setRegData({ username: '', email: '', password: '', confirmPassword: '' });
      setLoginMsg('');
      setRegMsg('');
      setIsLoading(false);
    }
  }, [visible]);

  // 登入提交
  const handleLogin = async (e) => {
  e.preventDefault();
  setLoginMsg('');
  setIsLoading(true);

  const result = await login(loginData);
  
  if (result.success) {
    onClose();
    setLoginData({ username: '', password: '' });
  } else {
    if (result.requiresVerification) {
      setLoginMsg(
        <div>
          <p>{result.message}</p>
          <p>請檢查 <strong>{result.email}</strong> 的收件匣。</p>
          <button 
            onClick={() => handleResendVerification(result.email)}
            style={{ marginTop: '10px', padding: '5px 10px', fontSize: '12px' }}
          >
            重新發送驗證郵件
          </button>
        </div>
      );
    } else {
      setLoginMsg(result.message);
    }
  }
  
  setIsLoading(false);
};

const handleResendVerification = async (email) => {
  try {
    const res = await fetch('http://localhost:8080/api/auth/resend-verification', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    });
    
    if (res.ok) {
      setLoginMsg('驗證郵件已重新發送，請檢查您的收件匣。');
    } else {
      setLoginMsg('重新發送失敗，請稍後再試。');
    }
  } catch (err) {
    setLoginMsg('網路錯誤，請稍後再試。');
  }
};


  // 註冊提交
  const handleRegister = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setRegMsg('');
    if (regData.password !== regData.confirmPassword) {
      setRegMsg('兩次密碼不一致');
      setIsLoading(false);
      return;
    }
    try {
      const res = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: regData.username,
          email: regData.email,
          password: regData.password,
        }),
      });
      const data = await res.json();
      if (res.ok) {
 
  if (data.requiresVerification) {
    setRegMsg(`註冊成功！驗證信已發送至 ${data.email}，請至信箱完成啟用。`);
  } else {
    setRegMsg('註冊成功，請登入！');
  }
  setTimeout(() => {
    setIsLogin(true);
    setRegData({ username: '', email: '', password: '', confirmPassword: '' });
    setRegMsg('');
  }, 2000);
} else {
  setRegMsg(data.message || '註冊失敗');
}
    } catch (err) {
      setRegMsg('網路錯誤，請稍後再試');
    }
    setIsLoading(false);
  };

  if (!visible) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{isLogin ? '會員登入' : '會員註冊'}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>
        {isLogin ? (
          <form onSubmit={handleLogin} className="login-form">
            <div className="form-group">
              <input
                type="text"
                name="username"
                value={loginData.username}
                onChange={e => setLoginData({ ...loginData, username: e.target.value })}
                placeholder="使用者名稱"
                required
                disabled={isLoading}
              />
            </div>
            <div className="form-group">
              <input
                type="password"
                name="password"
                value={loginData.password}
                onChange={e => setLoginData({ ...loginData, password: e.target.value })}
                placeholder="密碼"
                required
                disabled={isLoading}
              />
            </div>
            {loginMsg && <div className="message">{loginMsg}</div>}
            <button type="submit" disabled={isLoading} className="submit-btn">
              {isLoading ? '登入中...' : '登入'}
            </button>
            <div className="form-switch">
              沒有帳號？<span className="switch-link" onClick={() => setIsLogin(false)}>立即註冊</span>
            </div>
          </form>
        ) : (
          <form onSubmit={handleRegister} className="login-form">
            <div className="form-group">
              <input
                type="text"
                name="username"
                value={regData.username}
                onChange={e => setRegData({ ...regData, username: e.target.value })}
                placeholder="使用者名稱"
                required
                disabled={isLoading}
              />
            </div>
            <div className="form-group">
              <input
                type="email"
                name="email"
                value={regData.email}
                onChange={e => setRegData({ ...regData, email: e.target.value })}
                placeholder="Email"
                required
                disabled={isLoading}
              />
            </div>
            <div className="form-group">
              <input
                type="password"
                name="password"
                value={regData.password}
                onChange={e => setRegData({ ...regData, password: e.target.value })}
                placeholder="密碼"
                required
                disabled={isLoading}
              />
            </div>
            <div className="form-group">
              <input
                type="password"
                name="confirmPassword"
                value={regData.confirmPassword}
                onChange={e => setRegData({ ...regData, confirmPassword: e.target.value })}
                placeholder="再次輸入密碼"
                required
                disabled={isLoading}
              />
            </div>
            {regMsg && <div className="message">{regMsg}</div>}
            <button type="submit" disabled={isLoading} className="submit-btn">
              {isLoading ? '註冊中...' : '註冊'}
            </button>
            <div className="form-switch">
              已有帳號？<span className="switch-link" onClick={() => setIsLogin(true)}>返回登入</span>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
