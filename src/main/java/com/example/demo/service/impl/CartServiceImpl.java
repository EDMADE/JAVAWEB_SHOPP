package com.example.demo.service.impl;

import com.example.demo.model.dto.AddToCartDTO;
import com.example.demo.model.dto.CartResponseDTO;
import com.example.demo.model.dto.CartItemDTO;
import com.example.demo.model.entity.CartItem;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.ProductSku;
import com.example.demo.model.entity.ProductImage;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSkuRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    public CartResponseDTO getCart(Long userId) {
        System.out.println("=== 取得購物車，用戶ID: " + userId + " ===");
        
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        System.out.println("找到 " + cartItems.size() + " 個購物車項目");
        
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CartItem item : cartItems) {
            CartItemDTO dto = new CartItemDTO();
            dto.setCartItemId(item.getCartItemId());
            dto.setProductId(item.getProductId());
            dto.setSkuId(item.getSkuId());
            dto.setQuantity(item.getQuantity());
            
            
            
           
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                dto.setProductName(product.getName());
                
           
                String imageUrl = getProductImageUrl(product.getProductId(), item.getSkuId());
                dto.setImageUrl(imageUrl);
                dto.setProductImage(imageUrl);
                
                if (item.getSkuId() != null) {
                    ProductSku sku = productSkuRepository.findById(item.getSkuId()).orElse(null);
                    if (sku != null) {
                        dto.setPrice(sku.getPrice()); 
                        dto.setSkuSpec(sku.getSpecJson()); 
                        System.out.println("使用 SKU 價格: " + sku.getPrice() + ", 最終圖片: " + imageUrl);
                    } else {
                       
                        dto.setPrice(product.getCurrentPrice());
                        System.out.println("SKU 不存在，使用商品基本價格: " + product.getCurrentPrice());
                    }
                } else {
                    
                    dto.setPrice(product.getCurrentPrice());
                    System.out.println("單規格商品價格: " + product.getCurrentPrice() + ", 最終圖片: " + imageUrl);
                }
                
                if (dto.getPrice() != null) {
                    BigDecimal itemTotal = dto.getPrice().multiply(new BigDecimal(dto.getQuantity()));
                    totalAmount = totalAmount.add(itemTotal);
                }
                
            } else {
                System.err.println("❌ 找不到商品 ID: " + item.getProductId());
                dto.setProductName("商品不存在");
                dto.setPrice(BigDecimal.ZERO);
                dto.setProductImage("/uploads/default.png"); 
            }
            
            cartItemDTOs.add(dto);
        }
        
        CartResponseDTO response = new CartResponseDTO();
        response.setItems(cartItemDTOs);
        response.setTotalAmount(totalAmount.doubleValue());
        
        System.out.println("=== 購物車回應完成，共 " + cartItemDTOs.size() + " 個項目，總金額: " + totalAmount + " ===");
        return response;
    }

    private String getProductImageUrl(Long productId, Long skuId) {
        
        List<ProductImage> productImages = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        if (productImages != null && !productImages.isEmpty()) {
            String imageUrl = productImages.get(0).getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                System.out.println("使用多圖第一張: " + imageUrl);
                return imageUrl;
            }
        }
        
        
        if (skuId != null) {
            ProductSku sku = productSkuRepository.findById(skuId).orElse(null);
            if (sku != null && sku.getImageUrl() != null && !sku.getImageUrl().trim().isEmpty()) {
                System.out.println("使用 SKU 圖片: " + sku.getImageUrl());
                return sku.getImageUrl();
            }
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getMainImageUrl() != null && !product.getMainImageUrl().trim().isEmpty()) {
            System.out.println("使用商品主圖: " + product.getMainImageUrl());
            return product.getMainImageUrl();
        }
        System.out.println("使用預設圖片");
        return "/uploads/default.png";
    }

    @Override
    @Transactional
    public CartResponseDTO addToCart(Long userId, AddToCartDTO dto) {
        System.out.println("=== 加入購物車：用戶ID=" + userId + ", 商品ID=" + dto.getProductId() + ", SKU ID=" + dto.getSkuId() + ", 數量=" + dto.getQuantity() + " ===");
        Product product = productRepository.findById(dto.getProductId()).orElse(null);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + dto.getProductId());
        }
        
        if (dto.getSkuId() != null) {
            ProductSku sku = productSkuRepository.findById(dto.getSkuId()).orElse(null);
            if (sku == null) {
                throw new RuntimeException("商品規格不存在: " + dto.getSkuId());
            }
        }
        
       
        Optional<CartItem> existingItemOpt;
        if (dto.getSkuId() != null) {
            
            existingItemOpt = cartItemRepository.findByUserIdAndProductIdAndSkuId(
                userId, dto.getProductId(), dto.getSkuId());
        } else {
            
            existingItemOpt = cartItemRepository.findByUserIdAndProductIdAndSkuIdIsNull(
                userId, dto.getProductId());
        }
        
        if (existingItemOpt.isPresent()) {
            
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + dto.getQuantity());
            cartItemRepository.save(existingItem);
            System.out.println("✅ 更新現有項目數量: " + existingItem.getQuantity());
        } else {
           
            CartItem newItem = new CartItem();
            newItem.setUserId(userId);
            newItem.setProductId(dto.getProductId());
            newItem.setSkuId(dto.getSkuId());
            newItem.setQuantity(dto.getQuantity());
            newItem.setAddedTime(LocalDateTime.now());
            
            try {
                cartItemRepository.save(newItem);
                System.out.println("✅ 新增購物車項目成功");
            } catch (Exception e) {
                System.err.println("❌ 新增購物車項目失敗: " + e.getMessage());
                throw new RuntimeException("新增購物車失敗", e);
            }
        }
        
        return getCart(userId);
    }



    @Override
    @Transactional
    public CartResponseDTO updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        System.out.println("=== 更新購物車項目：用戶ID=" + userId + ", 項目ID=" + cartItemId + ", 數量=" + quantity + " ===");
        
        
        Optional<CartItem> itemOpt = cartItemRepository.findByCartItemIdAndUserId(cartItemId, userId);
        
        if (itemOpt.isPresent()) {
            CartItem item = itemOpt.get();
            if (quantity > 0) {
                item.setQuantity(quantity);
                cartItemRepository.save(item);
                System.out.println("✅ 更新商品數量: " + quantity);
            } else {
                cartItemRepository.delete(item);
                System.out.println("✅ 數量為 0，刪除項目");
            }
        } else {
            System.err.println("❌ 找不到購物車項目: " + cartItemId);
        }
        
        return getCart(userId);
    }
    
    @Transactional
    public void clearUserCart(Long userId) {
        try {
            System.out.println("🗑️ 開始清空用戶購物車: " + userId);
            
            List<CartItem> userCartItems = cartItemRepository.findByUserId(userId);
            int itemCount = userCartItems.size();
            if (itemCount > 0) {
                cartItemRepository.deleteByUserId(userId);
                System.out.println("✅ 已清空用戶購物車: " + itemCount + " 個項目");
            } else {
                System.out.println("ℹ️ 用戶購物車已經是空的");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 清空購物車失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("清空購物車失敗", e);
        }
    }



    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        System.out.println("=== 移除購物車項目：用戶ID=" + userId + ", 項目ID=" + cartItemId + " ===");
        
        int deletedCount = cartItemRepository.deleteByCartItemIdAndUserId(cartItemId, userId);
        if (deletedCount > 0) {
            System.out.println("✅ 移除購物車項目成功");
        } else {
            System.err.println("❌ 找不到要移除的購物車項目");
        }
    }
}


