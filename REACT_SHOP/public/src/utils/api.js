import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 10000,
});

api.interceptors.request.use(
  (config) => {
    console.log('📤 API請求:', config.method.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('📤 請求錯誤:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    console.log('📥 API回應:', response.status, response.config.url);
    return response;
  },
  (error) => {
    console.error('📥 回應錯誤:', error.response?.status, error.response?.data);
    
    if (error.response?.status === 401) {
      console.warn('⚠️ 401 Unauthorized - 用戶未登入或session過期');
      
      localStorage.removeItem('user');
      
      if (window.location.pathname !== '/') {
        window.location.href = '/';
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
