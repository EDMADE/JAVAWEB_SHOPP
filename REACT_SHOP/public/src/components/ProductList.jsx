import React, { useEffect } from 'react';
import ProductCard from './ProductCard';
import { imageCache } from '../utils/ImageCache';
import './ProductList.css';

export default function ProductList({ products, updateCartCount }) {
  useEffect(() => {
    products.forEach(product => {
      const rawImageUrl = product.mainImageUrl || product.main_image_url;
      if (rawImageUrl) {
        let imageUrl;
        if (rawImageUrl.startsWith('http')) {
          imageUrl = rawImageUrl;
        } else if (rawImageUrl.startsWith('/uploads')) {
          imageUrl = `http://localhost:8080${rawImageUrl}`;
        } else {
          imageUrl = rawImageUrl;
        }
        imageCache.preloadImage(imageUrl);
      }
    });
  }, [products]);

  if (!products || products.length === 0) {
    return (
      <div className="no-products">
        <p>目前沒有商品</p>
      </div>
    );
  }

  return (
 <div className="product-list">
  {products.map(product => (
    <div className="product-card-wrapper" key={product.id}>
      <ProductCard product={product} updateCartCount={updateCartCount} />
    </div>
  ))}
</div>

  );
}
