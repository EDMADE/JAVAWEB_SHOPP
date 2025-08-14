package com.example.demo.service;

import com.example.demo.model.dto.ProductCreateDTO;
import com.example.demo.model.dto.ProductDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.Product.ProductStatus;
import com.example.demo.model.entity.ProductImage;
import com.example.demo.model.entity.ProductSku;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSkuRepository;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductSkuService productSkuService;
    @Autowired
    private ProductImageRepository productImageRepository;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    public Product findById(Long productId) {
        return productRepository.findById(productId).orElse(null);
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    private BigDecimal stringToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("價格格式錯誤: " + value);
        }
    }
    
    private Integer stringToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("數量格式錯誤: " + value);
        }
    }
    
    // 優化後的 createProduct
    public Product createProductWithLocalImages(ProductCreateDTO dto, List<MultipartFile> images, boolean hasSkus) {
        System.out.println("=== 開始建立商品（本機圖片儲存）===");
        
        Product product = new Product();
        
        // 基本資料設定
        product.setSellerId(dto.getSeller_id());
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setProductCondition(dto.getProduct_condition());
        product.setDescription(dto.getDescription());
        product.setStatus(Product.ProductStatus.valueOf(
        	    dto.getStatus() != null ? dto.getStatus() : "ACTIVE"
        	));
        BigDecimal currentPrice;
        if (!hasSkus) {
            if (dto.getCurrent_price() == null || dto.getCurrent_price().trim().isEmpty()) {
                throw new IllegalArgumentException("價格不能為空");
            } 
            currentPrice = new BigDecimal(dto.getCurrent_price());
        } else {
            
            currentPrice = BigDecimal.ZERO;
        }
        product.setCurrentPrice(currentPrice);
        
        // 處理庫存
        Integer stockQuantity = stringToInteger(dto.getStock_quantity());
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        product.setStockQuantity(stockQuantity);
        
        //處理競標邏輯
        boolean isAuction = dto.getBid_end_time() != null && !dto.getBid_end_time().isEmpty();
        
        if (isAuction) {
            BigDecimal startPrice = stringToBigDecimal(dto.getStart_price());
            if (startPrice == null) {
                throw new RuntimeException("開啟競標時，起標價不得為空");
            }
            product.setStartPrice(startPrice);
            product.setBidEndTime(LocalDateTime.parse(dto.getBid_end_time()));
        } else {
            product.setStartPrice(product.getCurrentPrice());
            product.setBidEndTime(null);
        }
        
        
        if (images != null && !images.isEmpty()) {
            String imageUrl = saveImageToLocal(images.get(0)); 
            product.setMainImageUrl(imageUrl);
            System.out.println("圖片儲存成功: " + imageUrl);
        } else {
            product.setMainImageUrl("/uploads/default.png");
        }
        
        product.setStatus(Product.ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        
        if (dto.getSpecifications() != null && !dto.getSpecifications().trim().isEmpty()) {
            product.setSpecifications(dto.getSpecifications());
        }
        
        Product savedProduct = productRepository.save(product);
        System.out.println("商品建立成功，ID: " + savedProduct.getProductId());
        if (images != null && !images.isEmpty()) {
            saveProductImages(savedProduct.getProductId(), images);
        }
        return savedProduct;
    }

    
    private String cleanFileName(String filename) {
        if (filename == null) return "unknown.jpg";
        
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String saveImageToLocal(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String cleanedFilename = cleanFileName(originalFilename);
            String filename = System.currentTimeMillis() + "_" + cleanedFilename;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("圖片上傳失敗: " + file.getOriginalFilename(), e);
        }
    }

    private boolean isValidImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif") ||
            contentType.equals("image/webp")
        );
    }

    public void updateProductImages(Long productId, List<String> keepImageUrls, List<MultipartFile> newImages) {
    
        List<ProductImage> oldImages = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);

        
        for (ProductImage img : oldImages) {
            if (keepImageUrls == null || !keepImageUrls.contains(img.getImageUrl())) {
                productImageRepository.delete(img);
              
                try {
                    String fileName = img.getImageUrl().replace("/uploads/", "");
                    String uploadDir = System.getProperty("user.dir") + "/uploads";
                    Path filePath = Paths.get(uploadDir, fileName);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                } catch (Exception e) {
                 
                }
            }
        }

      
        int maxSortOrder = oldImages.stream()
            .filter(img -> keepImageUrls != null && keepImageUrls.contains(img.getImageUrl()))
            .mapToInt(ProductImage::getSortOrder).max().orElse(-1);

        if (newImages != null && !newImages.isEmpty()) {
            for (int i = 0; i < newImages.size(); i++) {
                MultipartFile image = newImages.get(i);
                String imageUrl = saveImageToLocal(image);
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImageUrl(imageUrl);
                productImage.setImageName(image.getOriginalFilename());
                productImage.setSortOrder(maxSortOrder + 1 + i);
                productImageRepository.save(productImage);
            }
        }
    }
    public Product updateProduct(Long id, ProductCreateDTO dto) {
        System.out.println("=== 更新商品 ID: " + id + " ===");
        
        Product product = findById(id);
        if (product == null) {
            System.out.println("找不到商品 ID: " + id);
            return null;
        }
        
     
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setCurrentPrice(stringToBigDecimal(dto.getCurrent_price()));
        product.setStockQuantity(stringToInteger(dto.getStock_quantity()));
        
      
        boolean isAuction = dto.getBid_end_time() != null && !dto.getBid_end_time().isEmpty();
        
        if (isAuction) {
            BigDecimal startPrice = stringToBigDecimal(dto.getStart_price());
            if (startPrice == null) {
                throw new RuntimeException("開啟競標時，起標價不得為空");
            }
            product.setStartPrice(startPrice);
            product.setBidEndTime(LocalDateTime.parse(dto.getBid_end_time()));
        } else {
            product.setStartPrice(product.getCurrentPrice());
            product.setBidEndTime(null);
        }
        
        // 更新規格
        if (dto.getSpecifications() != null) {
            product.setSpecifications(dto.getSpecifications());
        }
        
        Product updatedProduct = productRepository.save(product);
        System.out.println("商品更新成功");
        return updatedProduct;
    }
    @Transactional(readOnly = true)
    public List<ProductDTO> getActiveProductDTOs() {
        List<ProductDTO> dtos = productRepository.findActiveProducts();
        for (ProductDTO dto : dtos) {
            List<ProductSku> skus = productSkuRepository.findByProductId(dto.getId());
            dto.setSkus(skus);
            
            if (skus != null && !skus.isEmpty()) {
                int totalStock = skus.stream().mapToInt(ProductSku::getStock).sum();
                dto.setStockQuantity(totalStock);
                BigDecimal minPrice = skus.stream().map(ProductSku::getPrice).min(BigDecimal::compareTo).orElse(dto.getCurrentPrice());
                BigDecimal maxPrice = skus.stream().map(ProductSku::getPrice).max(BigDecimal::compareTo).orElse(dto.getCurrentPrice());
                dto.setMinPrice(minPrice);
                dto.setMaxPrice(maxPrice);
            } else {
                dto.setMinPrice(dto.getCurrentPrice());
                dto.setMaxPrice(dto.getCurrentPrice());
            }
            
          
            List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(dto.getId());
            dto.setProductImages(images);
        }
        return dtos;
    }

    public List<ProductDTO> getMyProductDTOs(Long userId) {
        List<ProductDTO> dtos = productRepository.findDTOsBySellerId(userId);
        for (ProductDTO dto : dtos) {
            List<ProductSku> skus = productSkuRepository.findByProductId(dto.getId());
            dto.setSkus(skus);
            if (skus != null && !skus.isEmpty()) {
                BigDecimal minPrice = skus.stream().map(ProductSku::getPrice).min(BigDecimal::compareTo).orElse(dto.getCurrentPrice());
                BigDecimal maxPrice = skus.stream().map(ProductSku::getPrice).max(BigDecimal::compareTo).orElse(dto.getCurrentPrice());
                dto.setMinPrice(minPrice);
                dto.setMaxPrice(maxPrice);
            } else {
                dto.setMinPrice(dto.getCurrentPrice());
                dto.setMaxPrice(dto.getCurrentPrice());
            }
            List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(dto.getId());
            dto.setProductImages(images);
        }
        return dtos;
    }

   
    public Product createProductWithoutImages(ProductCreateDTO dto) {
        return createProductWithLocalImages(dto, null, false);
    }

    public List<Product> createProducts(List<ProductCreateDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            System.out.println("批量創建：輸入列表為空");
            return new ArrayList<>();
        }
        System.out.println("批量創建 " + dtos.size() + " 個商品");
        return dtos.stream()
            .filter(Objects::nonNull)
            .map(dto -> createProductWithLocalImages(dto, null, false))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

   
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getMyProducts(Long userId) {
        List<Product> products = productRepository.findBySellerId(userId);
        System.out.println("使用者 " + userId + " 的商品數量: " + products.size());
        return products;
    }
 
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO(
            product.getProductId(), product.getSellerId(), product.getName(), product.getCategory(),
            product.getDescription(), product.getStartPrice(), product.getCurrentPrice(),
            product.getStockQuantity(), product.getMainImageUrl(), product.getBidEndTime(),
            product.getStatus().toString(), product.getCreatedAt(), product.getSpecifications(),
            product.getProductCondition()
        );
        
       
        List<ProductSku> skus = productSkuRepository.findByProductId(product.getProductId());
        dto.setSkus(skus);
        
     
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getProductId());
        dto.setProductImages(images);
        
        return dto;
    }

   
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        List<Product> products = productRepository.findByCategory(category);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // 編輯商品
    @Transactional
    public Product updateProduct(Long id, ProductCreateDTO dto, List<MultipartFile> images, boolean hasSkus, String skusJson) {
        try {
            
            Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            //更新基本資料
            existingProduct.setName(dto.getName());
            existingProduct.setCategory(dto.getCategory());
            existingProduct.setProductCondition(dto.getProduct_condition());
            existingProduct.setDescription(dto.getDescription());
            existingProduct.setStatus(Product.ProductStatus.ACTIVE);
            
           
            if (!hasSkus) {
                //單一規格商品，價格庫存必填
                if (dto.getCurrent_price() == null || dto.getCurrent_price().trim().isEmpty()) {
                    throw new IllegalArgumentException("價格不能為空");
                }
                existingProduct.setCurrentPrice(new BigDecimal(dto.getCurrent_price()));
                existingProduct.setStockQuantity(Integer.parseInt(dto.getStock_quantity()));
            } else {
                //多規格商品，主商品價格庫存設為 0
                existingProduct.setCurrentPrice(BigDecimal.ZERO);
                existingProduct.setStockQuantity(0);
            }
            
            
            if (images != null && !images.isEmpty()) {
                String imageUrl = saveImageToLocal(images.get(0));
                existingProduct.setMainImageUrl(imageUrl);
            }
            
            
            if (dto.getSpecifications() != null && !dto.getSpecifications().trim().isEmpty()) {
                existingProduct.setSpecifications(dto.getSpecifications());
            }
            
            
            Product savedProduct = productRepository.save(existingProduct);
            
            //處理 SKU 更新
            if (hasSkus && skusJson != null && !skusJson.trim().isEmpty()) {
               
                productSkuService.deleteSkusByProductId(id);
              
                productSkuService.saveSkuList(skusJson, id);
            }
            
            return savedProduct;
        } catch (Exception e) {
            throw new RuntimeException("商品更新失敗: " + e.getMessage(), e);
        }
    }

//    public ProductDTO getProductDTOById(Long id) {
//        Product product = productRepository.findById(id).orElse(null);
//        if (product == null) return null;
//        ProductDTO dto = new ProductDTO(
//            product.getId(), product.getSellerId(), product.getName(), product.getCategory(),
//            product.getDescription(), product.getStartPrice(), product.getCurrentPrice(),
//            product.getStockQuantity(), product.getMainImageUrl(), product.getBidEndTime(),
//            product.getStatus().toString(), product.getCreatedAt(), product.getSpecifications(),
//            product.getProductCondition()
//        );
//        List<ProductSku> skus = productSkuRepository.findByProductId(id);
//        dto.setSkus(skus);
//        return dto;
//    }
    @Transactional
    public Product updateProductStatus(Long productId, String newStatus) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到商品 ID: " + productId));
        
      
        try {
            ProductStatus statusEnum = ProductStatus.valueOf(newStatus.toUpperCase());
            product.setStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("無效的狀態值: " + newStatus);
        }
        
        return productRepository.save(product);
    }


    
    public void saveProductImages(Long productId, List<MultipartFile> images) {
        System.out.println("=== 開始儲存商品圖片，商品ID: " + productId + " ===");
        
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            try {
                
                String imageUrl = saveImageToLocal(image);
                
               
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImageUrl(imageUrl);
                productImage.setImageName(image.getOriginalFilename());
                productImage.setSortOrder(i); // 按上傳順序排列
                
                productImageRepository.save(productImage);
                System.out.println("✅ 圖片 " + (i+1) + " 儲存成功: " + imageUrl);
                
            } catch (Exception e) {
                System.err.println("❌ 圖片 " + (i+1) + " 儲存失敗: " + e.getMessage());
                throw new RuntimeException("圖片儲存失敗: " + image.getOriginalFilename(), e);
            }
        }
        
        System.out.println("=== 所有圖片儲存完成 ===");
    }

   
    public List<ProductImage> getProductImages(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
    }

    
    public void replaceProductImages(Long productId, List<MultipartFile> newImages) {
        try {
          
            productImageRepository.deleteByProductId(productId);
            System.out.println("✅ 舊圖片刪除完成");
            
            //上傳新圖片
            saveProductImages(productId, newImages);
            
        } catch (Exception e) {
            throw new RuntimeException("替換商品圖片失敗", e);
        }
    }

 
    public ProductDTO getProductDTOById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;
        
        ProductDTO dto = new ProductDTO(
            product.getProductId(), product.getSellerId(), product.getName(), product.getCategory(),
            product.getDescription(), product.getStartPrice(), product.getCurrentPrice(),
            product.getStockQuantity(), product.getMainImageUrl(), product.getBidEndTime(),
            product.getStatus().toString(), product.getCreatedAt(), product.getSpecifications(),
            product.getProductCondition()
        );
        
       
        List<ProductSku> skus = productSkuRepository.findByProductId(id);
        dto.setSkus(skus);
        
     
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(dto.getId());
        dto.setProductImages(images);
        
        
        
        return dto;
    }

    @Transactional
    public boolean deleteMyProduct(Long productId, Long userId) {
        System.out.println("嘗試刪除商品 ID: " + productId + "，使用者: " + userId);
        
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                System.out.println("商品不存在");
                return false;
            }
            
            Product product = productOpt.get();
            if (!product.getSellerId().equals(userId)) {
                System.out.println("權限不足：商品不屬於該使用者");
                return false;
            }
            
            
            productImageRepository.deleteByProductId(productId);
            System.out.println("✅ 商品圖片刪除完成");
            
          
            List<ProductSku> skus = productSkuRepository.findByProductId(productId);
            if (!skus.isEmpty()) {
                productSkuRepository.deleteByProductId(productId);
                System.out.println("✅ SKU 刪除完成");
            }
            
          
            productRepository.deleteById(productId);
            System.out.println("✅ 商品刪除成功");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 刪除商品失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("刪除商品失敗: " + e.getMessage());
        }
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

}
