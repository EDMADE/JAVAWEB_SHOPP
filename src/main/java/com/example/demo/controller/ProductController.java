package com.example.demo.controller;

import com.example.demo.model.dto.ProductCreateDTO;
import com.example.demo.model.dto.ProductDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProductSkuService;

import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductSkuService productSkuService;

    
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        try {
            List<ProductDTO> products = productService.getActiveProductDTOs();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.err.println("❌ 查詢商品列表失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ProductDTO>> getActiveProducts() {
        try {
            List<ProductDTO> products = productService.getActiveProductDTOs();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.err.println("❌ 查詢活躍商品失敗: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProductDTO>> getMyProducts(@RequestParam Long userId) {
        try {
            List<ProductDTO> products = productService.getMyProductDTOs(userId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.err.println("❌ 查詢我的商品失敗: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    //商品詳情
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        try {
            ProductDTO dto = productService.getProductDTOById(id);
            return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("❌ 查詢商品詳情失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    //分類查詢
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        try {
            List<ProductDTO> products = productService.getProductsByCategory(category);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.err.println("❌ 查詢分類商品失敗: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    //創建商品
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
        @Valid @ModelAttribute ProductCreateDTO dto,
        @RequestParam(value = "images", required = false) List<MultipartFile> images,
        @RequestParam(value = "skus", required = false) String skusJson
    ) {
        try {
            System.out.println("=== 收到的商品資料 ===");
            System.out.println("name: " + dto.getName());
            System.out.println("seller_id: " + dto.getSeller_id());
            System.out.println("current_price: " + dto.getCurrent_price());
            System.out.println("stock_quantity: " + dto.getStock_quantity());
            System.out.println("圖片數量: " + (images != null ? images.size() : 0));
            System.out.println("SKU 資料: " + skusJson);
            
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "圖片上傳失敗",
                    "message", "至少需要上傳一張商品圖片"
                ));
            }

            boolean hasSkus = (skusJson != null && !skusJson.trim().isEmpty() && !skusJson.equals("[]") && !skusJson.equals("null"));
            
            Product product = productService.createProductWithLocalImages(dto, null, hasSkus);
            System.out.println("✅ 商品主表建立成功，ID: " + product.getProductId());

            productService.saveProductImages(product.getProductId(), images);
            System.out.println("✅ 多圖片儲存完成，共 " + images.size() + " 張");

            if (hasSkus) {
                System.out.println("🎯 檢測到多規格商品，開始建立 SKU 資料");
                productSkuService.saveSkuList(skusJson, product.getProductId());
                System.out.println("✅ SKU 資料建立完成");
            } else {
                System.out.println("📦 單一規格商品，使用統一價格庫存");
                System.out.println("統一價格: " + product.getCurrentPrice());
                System.out.println("統一庫存: " + product.getStockQuantity());
            }

            return ResponseEntity.ok().body(Map.of(
                "message", "商品上架成功",
                "productId", product.getProductId(),
                "hasSkus", hasSkus,
                "imageCount", images.size()
            ));

        } catch (Exception e) {
            System.err.println("❌ 商品上架失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "上架失敗",
                "message", e.getMessage()
            ));
        }
    }

    // ✅ 更新商品（需要認證）
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
        @PathVariable Long id,
        @Valid @ModelAttribute ProductCreateDTO dto,
        @RequestParam(value = "images", required = false) List<MultipartFile> images,
        @RequestParam(value = "keepImages", required = false) List<String> keepImages,
        @RequestParam(value = "skus", required = false) String skusJson
    ) {
        try {
            boolean hasSkus = (skusJson != null && !skusJson.trim().isEmpty() && !skusJson.equals("[]") && !skusJson.equals("null"));
            Product updated = productService.updateProduct(id, dto, null, hasSkus, skusJson);

           
            productService.updateProductImages(id, keepImages, images);

            return ResponseEntity.ok().body(Map.of(
                "message", "商品更新成功",
                "productId", id,
                "hasSkus", hasSkus
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "更新失敗",
                "message", e.getMessage()
            ));
        }
    }

    //刪除商品
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, 
                                             @RequestParam Long userId) {
        try {
            boolean deleted = productService.deleteMyProduct(id, userId);
            return deleted ? 
                ResponseEntity.ok().build() : 
                ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    //更新商品狀態
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null) {
                return ResponseEntity.badRequest().body("狀態不能為空");
            }
            
            if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) {
                return ResponseEntity.badRequest().body("無效的狀態值");
            }
            
            Product updated = productService.updateProductStatus(id, newStatus);
            return ResponseEntity.ok(Map.of(
                "message", "狀態更新成功",
                "productId", id,
                "newStatus", updated.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "狀態更新失敗",
                "message", e.getMessage()
            ));
        }
    }
}
