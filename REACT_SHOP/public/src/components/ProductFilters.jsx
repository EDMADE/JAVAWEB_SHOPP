import React from 'react';
import './ProductFilters.css';

export default function ProductFilters({ 
  filters, 
  setFilters, 
  sortBy, 
  setSortBy, 
  sortOptions, 
  onReset 
}) {

  const conditionOptions = [
    { value: '', label: '全部狀況' },
    { value: '全新', label: '全新' },
    { value: '二手', label: '二手' },
  ];

  return (
    <div className="product-filters">
      <div className="filters-container">
        <h3 className="filters-title">篩選商品</h3>
        
        <div className="filters-row">
          <div className="filter-group">
            <label>價格範圍</label>
            <div className="price-range">
              <input
                type="number"
                placeholder="最低價"
                value={filters.priceRange.min}
                onChange={e => setFilters(prev => ({
                  ...prev,
                  priceRange: { ...prev.priceRange, min: e.target.value }
                }))}
              />
              <span>-</span>
              <input
                type="number"
                placeholder="最高價"
                value={filters.priceRange.max}
                onChange={e => setFilters(prev => ({
                  ...prev,
                  priceRange: { ...prev.priceRange, max: e.target.value }
                }))}
              />
            </div>
          </div>

          <div className="filter-group">
            <label>商品狀況</label>
            <select
              value={filters.condition}
              onChange={e => setFilters(prev => ({ ...prev, condition: e.target.value }))}
            >
              {conditionOptions.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label>排序方式</label>
            <select value={sortBy} onChange={e => setSortBy(e.target.value)}>
              {sortOptions.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <button className="reset-filters-btn" onClick={onReset}>
              重設篩選
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
