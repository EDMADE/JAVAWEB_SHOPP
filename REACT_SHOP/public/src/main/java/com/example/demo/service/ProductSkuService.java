package com.example.demo.service;

import com.example.demo.model.entity.ProductSku;
import com.example.demo.repository.ProductSkuRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSkuService {
    private final ProductSkuRepository productSkuRepository;
    private String stableStringify(Map<String, Object> spec) throws Exception {
        return new ObjectMapper().writeValueAsString(new java.util.TreeMap<>(spec));
    }

    @Transactional
    public void saveSkuList(String skusJson, Long productId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> skuArr = mapper.readValue(skusJson, new TypeReference<>(){});
            
            if (skuArr.isEmpty()) {
                System.out.println("SKU 陣列為空，跳過建立");
                return;
            }

            List<ProductSku> skuList = new ArrayList<>();
            for (Map<String, Object> skuMap : skuArr) {
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);
                sku.setSpecJson(stableStringify((Map<String, Object>) skuMap.get("spec")));
                sku.setPrice(new BigDecimal(skuMap.get("price").toString()));
                sku.setStock(Integer.parseInt(skuMap.get("stock").toString()));
                
            
                if (skuMap.containsKey("imageUrl")) {
                    sku.setImageUrl(skuMap.get("imageUrl").toString());
                }
                
                skuList.add(sku);
                System.out.println("建立 SKU: " + sku.getSpecJson() + 
                                 ", 價格: " + sku.getPrice() + 
                                 ", 庫存: " + sku.getStock());
            }
            
            productSkuRepository.saveAll(skuList);
            System.out.println("✅ 成功建立 " + skuList.size() + " 個 SKU");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("SKU 批量儲存失敗: " + e.getMessage(), e);
        }
    }

    public List<ProductSku> getSkusByProductId(Long productId) {
        return productSkuRepository.findByProductId(productId);
    }

    @Transactional
    public void deleteSkusByProductId(Long productId) {
        productSkuRepository.deleteByProductId(productId);
    }
}
