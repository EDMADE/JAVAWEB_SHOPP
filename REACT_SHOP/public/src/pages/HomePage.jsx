import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import Header from '../components/Header';
import CategoryNav from '../components/CategoryNav';
import ProductFilters from '../components/ProductFilters';
import ProductList from '../components/ProductList';
import LoginModal from '../components/LoginModal';
import Footer from '../components/Footer';
import './HomePage.css';

export default function HomePage({ onCartClick, cartCount = 0, updateCartCount }) {
  const { isLoggedIn, loading: authLoading } = useAuth();
  const [products, setProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [categories] = useState([
    { id: 1, name: '電子產品' },
    { id: 2, name: '服飾' },
    { id: 3, name: '居家' },
  ]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showLogin, setShowLogin] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [loginTab, setLoginTab] = useState('login');

  const [filters, setFilters] = useState({
    priceRange: { min: '', max: '' },
    status: '', 
    stockStatus: '',
    condition: '',
  });
  const [sortBy, setSortBy] = useState('created_desc');

  const sortOptions = [
    { value: 'created_desc', label: '上架日期：新到舊' },
    { value: 'created_asc', label: '上架日期：舊到新' },
    { value: 'price_desc', label: '價格：高到低' },
    { value: 'price_asc', label: '價格：低到高' },
    { value: 'name_asc', label: '商品名稱：A-Z' },
    { value: 'popularity_desc', label: '人氣：高到低' },
    { value: 'rating_desc', label: '評分：高到低' },
  ];

  useEffect(() => {
    let ignore = false;
    
    if (products.length === 0) {
      setProductsLoading(true);
    }
    
    fetch('http://localhost:8080/api/products', {
      credentials: 'include',
    })
      .then(res => {
        if (!res.ok) throw new Error('API error');
        return res.json();
      })
      .then(data => {
        if (!ignore) {
          setProducts(Array.isArray(data) ? data : []);
        }
      })
      .catch(err => {
        console.error('載入商品失敗:', err);
        if (!ignore) {
          setProducts([]);
        }
      })
      .finally(() => {
        if (!ignore) {
          setProductsLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [isLoggedIn]);

  function getMinPrice(product) {
  if (product.skus && Array.isArray(product.skus) && product.skus.length > 0) {
    const prices = product.skus.map(s => Number(s.price)).filter(n => n > 0);
    if (prices.length > 0) return Math.min(...prices);
  }
  return Number(product.currentPrice ?? product.current_price ?? product.price ?? product.startPrice ?? product.start_price ?? 0);
}

function getMaxPrice(product) {
  if (product.skus && Array.isArray(product.skus) && product.skus.length > 0) {
    const prices = product.skus.map(s => Number(s.price)).filter(n => n > 0);
    if (prices.length > 0) return Math.max(...prices);
  }
  return Number(product.currentPrice ?? product.current_price ?? product.price ?? product.startPrice ?? product.start_price ?? 0);
}


  const getFilteredAndSortedProducts = () => {
    let filtered = products.filter(p => {
      const matchesCategory = !selectedCategory || p.category === selectedCategory;
      const matchesSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase());
      const price =getMinPrice(p);
      const matchesMinPrice = !filters.priceRange.min || price >= Number(filters.priceRange.min);
      const matchesMaxPrice = !filters.priceRange.max || price <= Number(filters.priceRange.max);
      const matchesStatus = !filters.status || p.status === filters.status;
      const stock = Number(p.stockQuantity || p.stock_quantity || 0);
      const matchesStock = !filters.stockStatus || 
                          (filters.stockStatus === 'IN_STOCK' && stock > 0) ||
                          (filters.stockStatus === 'OUT_OF_STOCK' && stock <= 0);
      const condition = p.productCondition ?? p.product_condition ?? '';
      const matchesCondition = !filters.condition || condition === filters.condition;
      
      return matchesCategory && matchesSearch && matchesMinPrice && 
             matchesMaxPrice && matchesStatus && matchesStock && matchesCondition;
    });
    filtered.sort((a, b) => {
      const minPriceA = getMinPrice(a);
      const minPriceB = getMinPrice(b);
      const maxPriceA = getMaxPrice(a);
      const maxPriceB = getMaxPrice(b);

      const dateA = new Date(a.createdAt || a.created_at);
      const dateB = new Date(b.createdAt || b.created_at);

      switch (sortBy) {
        case 'created_desc':
          return dateB - dateA;
        case 'created_asc':
          return dateA - dateB;
        case 'price_desc':
           return maxPriceB - maxPriceA;
        case 'price_asc':
           return minPriceA - minPriceB;
        case 'name_asc':
          return a.name.localeCompare(b.name);
        case 'popularity_desc':
          return (b.popularity || 0) - (a.popularity || 0);
        case 'rating_desc':
          return (b.rating || 0) - (a.rating || 0);
        default:
          return 0;
      }
    });

    return filtered;
  };

  const resetFilters = () => {
    setFilters({
      priceRange: { min: '', max: '' },
      status: '',
      stockStatus: '',
      condition: '',
    });
    setSortBy('created_desc');
    setSelectedCategory('');
    setSearchTerm('');
  };

  const handleLoginClick = () => {
    setLoginTab('login');
    setShowLogin(true);
  };

  const handleRegisterClick = () => {
    setLoginTab('register');
    setShowLogin(true);
  };

  const handleCloseModal = () => {
    setShowLogin(false);
    setTimeout(() => {
      setLoginTab('login');
    }, 200);
  };

  const filteredProducts = getFilteredAndSortedProducts();

  if (authLoading || (productsLoading && products.length === 0)) {
    return <div className="loading">載入中...</div>;
  }

  return (
    <div className="homepage-container">
      <Header
        onSearch={setSearchTerm}
        onLoginClick={handleLoginClick}
        onRegisterClick={handleRegisterClick}
        onCartClick={onCartClick}
        cartCount={cartCount}
      />

      <div className="content-wrapper">
        <div className="section-divider" />
        <section className="function-section">
          <CategoryNav
            categories={categories}
            selected={selectedCategory}
            onSelect={cat => setSelectedCategory(cat)}
          />
        </section>

        <div className="section-divider" />
        <section className="filter-section">
          <ProductFilters
            filters={filters}
            setFilters={setFilters}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortOptions={sortOptions}
            onReset={resetFilters}
          />
        </section>

        <div className="section-divider" />
        <section className="product-section">
          <div className="product-section-header">
            <h2 className="product-section-title">
              {selectedCategory
                ? `「${categories.find(c => c.name === selectedCategory)?.name}」商品`
                : '全部商品'}
            </h2>
            <span className="product-count">共 {filteredProducts.length} 件商品</span>
          </div>
          
          <ProductList 
            products={filteredProducts} 
            updateCartCount={updateCartCount}
          />
          
          {filteredProducts.length === 0 && !productsLoading && (
            <div className="no-products">
              <p>沒有符合條件的商品</p>
              <button onClick={resetFilters} className="reset-btn">重設篩選條件</button>
            </div>
          )}

          {productsLoading && products.length > 0 && (
            <div className="updating-indicator">更新中...</div>
          )}
        </section>
      </div>

      <LoginModal
        visible={showLogin}
        onClose={handleCloseModal}
        defaultTab={loginTab}
      />
      <Footer />
    </div>
  );
}
