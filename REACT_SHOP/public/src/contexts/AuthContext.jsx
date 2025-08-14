import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [loading, setLoading] = useState(true);
  const handleUnauthorized = () => {
    console.log('🚫 檢測到 401，清除登入狀態');
    setUser(null);
    setIsLoggedIn(false);
  };

  const checkAuthStatus = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/auth/status', { 
        credentials: 'include' 
      });
      
      if (res.ok) {
        const data = await res.json();
        if (data.loggedIn) {
          setUser({
            userId: data.userId,
            username: data.username,
            email: data.email
          });
          setIsLoggedIn(true);
          console.log('✅ 登入狀態確認:', data);
        } else {
          handleUnauthorized();
        }
      } else if (res.status === 401) {
        handleUnauthorized();
      }
    } catch (error) {
      console.error('檢查登入狀態失敗:', error);
      handleUnauthorized();
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials) => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      });
      
      if (response.ok) {
        const userData = await response.json();
        setUser({
          userId: userData.userId,
          username: userData.username,
          email: userData.email,
          emailVerified: userData.emailVerified
        });
        setIsLoggedIn(true);
        console.log('✅ 登入成功:', userData);
        
        return { success: true };
      } else {
        const errorData = await response.json();
        console.error('❌ 登入失敗:', errorData);
        
        if (response.status === 403 && errorData.requiresVerification) {
          return { 
            success: false, 
            message: errorData.message,
            requiresVerification: true,
            email: errorData.email
          };
        }
        
        return { success: false, message: errorData.message || '登入失敗' };
      }
    } catch (error) {
      console.error('❌ 登入錯誤:', error);
      return { success: false, message: '網路錯誤，請稍後再試' };
    }
  };

  const logout = async () => {
    try {
      console.log('🚪 開始登出...');
      
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      });
      
      console.log('✅ 登出請求完成');
    } catch (error) {
      console.error('❌ 登出請求失敗:', error);
    } finally {
      handleUnauthorized();
      console.log('✅ 登出完成，購物車將自動清空');
    }
  };

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const value = {
    user,
    isLoggedIn,
    loading,
    login,
    logout,
    handleUnauthorized,
    checkAuthStatus
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
