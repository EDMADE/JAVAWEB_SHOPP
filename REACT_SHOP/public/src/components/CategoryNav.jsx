import React from 'react';
import './CategoryNav.css';

export default function CategoryNav({ categories, selected, onSelect }) {
  return (
    <div className="category-nav">
      <div className="category-nav-container">
        <div className="category-buttons">
          <button
            className={!selected ? "category-btn active" : "category-btn"}
            onClick={() => onSelect("")}
          >
            全部商品
          </button>
          {categories.map(category => (
            <button
              key={category.id}
              className={selected === category.name ? "category-btn active" : "category-btn"}
              onClick={() => onSelect(category.name)}
            >
              {category.name}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
