import React, { createContext, useContext, useState, useEffect } from "react";
import api from "../utils/api";
import { useAuth } from "./AuthContext";

const CartContext = createContext();

export function useCart() {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}

export function CartProvider({ children }) {
  const { isLoggedIn, user, handleUnauthorized } = useAuth();
  const [cartItems, setCartItems] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);


  const fetchCart = async () => {
    if (!isLoggedIn || !user?.userId) {
      console.log('用戶未登入，清空購物車');
      setCartItems([]);
      setCartCount(0);
      setError(null);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      console.log('載入購物車，用戶ID:', user.userId);
      
      const res = await api.get("/api/cart");
      const items = res.data?.items || [];
      
      setCartItems(items);
      setCartCount(items.reduce((sum, item) => sum + (item.quantity || 0), 0));
      console.log('✅ 購物車載入成功，商品數量:', items.length);
      
    } catch (err) {
      console.error('載入購物車失敗:', err);
      
      if (err.response?.status === 401) {
        console.log('購物車 401，觸發登出');
        handleUnauthorized();
      }
      
      setCartItems([]);
      setCartCount(0);
      setError(err.response?.data?.error || '載入購物車失敗');
    } finally {
      setLoading(false);
    }
  };


  const updateCartItem = async (cartItemId, quantity) => {
    if (quantity < 1) return;
    
    try {
      console.log('🔄 更新購物車項目:', cartItemId, quantity);
      await api.put(`/api/cart/${cartItemId}?quantity=${quantity}`);
      

      await fetchCart();
      console.log('✅ 購物車項目更新完成');
    } catch (err) {
      console.error('❌ 更新購物車失敗:', err);
      if (err.response?.status === 401) {
        handleUnauthorized();
      }
      throw err;
    }
  };


  const removeCartItem = async (cartItemId) => {
    try {
      console.log('🗑️ 移除購物車項目:', cartItemId);
      await api.delete(`/api/cart/${cartItemId}`);
      

      await fetchCart();
      console.log('✅ 購物車項目移除完成');
    } catch (err) {
      console.error('❌ 移除購物車失敗:', err);
      if (err.response?.status === 401) {
        handleUnauthorized();
      }
      throw err;
    }
  };

 const clearCart = async () => {
    try {
      console.log('🗑️ 開始清空購物車');
      
      setCartItems([]);
      setCartCount(0);
      setError(null);
      setLoading(false);
      
      if (isLoggedIn && user?.userId) {
        try {
          await api.delete("/api/cart/clear");
          console.log('✅ 後端購物車已清空');
        } catch (err) {
          console.error('❌ 清空後端購物車失敗:', err);
          if (err.response?.status === 401) {
            handleUnauthorized();
          }
        }
      }
      
      console.log('✅ 購物車清空完成');
    } catch (err) {
      console.error('❌ 清空購物車失敗:', err);
    }
  };

  const syncCartCount = async () => {
    if (!isLoggedIn || !user?.userId) {
      setCartCount(0);
      return;
    }
    
    try {
      const res = await api.get("/api/cart");
      const items = res.data?.items || [];
      const totalCount = items.reduce((sum, item) => sum + (item.quantity || 0), 0);
      setCartCount(totalCount);
      console.log('✅ 購物車數量同步完成:', totalCount);
    } catch (err) {
      console.error('同步購物車數量失敗:', err);
      if (err.response?.status === 401) {
        handleUnauthorized();
      }
    }
  };

  const addToCart = async (productData) => {
    if (!isLoggedIn) {
      throw new Error('請先登入');
    }

    try {
      console.log('🛒 開始加入購物車:', productData);
      const res = await api.post("/api/cart", productData);
      await fetchCart();
      console.log('✅ 商品已加入購物車，數量已更新');
      return res.data;
    } catch (err) {
      if (err.response?.status === 401) {
        handleUnauthorized();
      }
      throw err;
    }
  };

  useEffect(() => {
    console.log('認證狀態變化:', { isLoggedIn, userId: user?.userId });
    
    if (isLoggedIn && user?.userId) {
      fetchCart();
    } else {
      clearCart();
    }
  }, [isLoggedIn, user?.userId]);

  const value = {
    cartItems,
    cartCount,
    loading,
    error,
    fetchCart,
    syncCartCount,
    clearCart,
    addToCart,
    updateCartItem,
    removeCartItem,
    setCartItems,
    setCartCount
  };

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
}
