import React, { useEffect, useState, useRef, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../utils/api";
import "./AddProduct.css";

const CATEGORY_OPTIONS = ["電子產品", "服飾", "居家", "美妝", "書籍", "戶外", "其他"];
const CONDITION_OPTIONS = ["全新", "二手"];

function cartesian(arr) {
  if (!arr.length) return [];
  return arr.reduce((a, b) =>
    a.flatMap(d => b.options.filter(Boolean).map(e => [...d, { [b.name]: e }]))
  , [[]]);
}

function stableStringify(obj) {
  return JSON.stringify(
    Object.keys(obj).sort().reduce((acc, key) => {
      acc[key] = obj[key];
      return acc;
    }, {})
  );
}

export default function EditProduct() {
  const { id } = useParams();
  const navigate = useNavigate();
  const fileInputRef = useRef();

  const [form, setForm] = useState({
    name: "", category: "", product_condition: "", description: "",
    current_price: "", stock_quantity: "", specifications: "",
  });
  
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);
  const [originalImages, setOriginalImages] = useState([]);
  
  const [specs, setSpecs] = useState([{ name: "", options: [""] }]);
  const [skuList, setSkuList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 載入商品資料
  useEffect(() => {
    api.get(`/api/products/${id}`)
      .then(res => {
        const prod = res.data;
        console.log('API 回傳商品資料:', prod);
        
        setForm({
          name: prod.name || "",
          category: prod.category || "",
          product_condition: prod.productCondition === "new" ? "全新"
                          : prod.productCondition === "used" ? "二手"
                          : prod.productCondition || "",
          description: prod.description || "",
          current_price: prod.currentPrice || prod.current_price || "",
          stock_quantity: prod.stockQuantity || prod.stock_quantity || "",
          specifications: prod.specifications || "",
        });

        const imageUrls = [];
        const originalImageUrls = [];
        
        if (prod.productImages && prod.productImages.length > 0) {
          prod.productImages.forEach(img => {
            const imageUrl = img.imageUrl;
            const fullUrl = imageUrl.startsWith('/') ? `http://localhost:8080${imageUrl}` : imageUrl;
            imageUrls.push(fullUrl);
            originalImageUrls.push(imageUrl);
          });
        } 
        else if (prod.mainImageUrl || prod.main_image_url) {
          const imageUrl = prod.mainImageUrl || prod.main_image_url;
          const fullUrl = imageUrl.startsWith('/') ? `http://localhost:8080${imageUrl}` : imageUrl;
          imageUrls.push(fullUrl);
          originalImageUrls.push(imageUrl);
        }
        
        setImagePreviews(imageUrls);
        setOriginalImages(originalImageUrls);
        console.log('載入的圖片預覽:', imageUrls);
        console.log('原有圖片URL:', originalImageUrls);

        setSpecs(prod.specifications ? JSON.parse(prod.specifications) : [{ name: "", options: [""] }]);
        setSkuList(
          (prod.skus || []).map(sku => ({
            ...sku,
            spec: typeof sku.spec === "object" ? sku.spec : JSON.parse(sku.specJson || "{}")
          }))
        );
        setLoading(false);
      })
      .catch(error => {
        console.error("載入商品失敗:", error);
        setError("載入商品失敗");
        setLoading(false);
      });
  }, [id]);

  const handleImageChange = e => {
    const files = Array.from(e.target.files);
    if (!files.length) return;
    
    const totalCurrentImages = imagePreviews.length;
    const availableSlots = 8 - totalCurrentImages;
    const filesToAdd = files.slice(0, availableSlots);
    
    if (filesToAdd.length === 0) {
      alert('最多只能上傳8張圖片');
      return;
    }
    
    const newFiles = [...imageFiles, ...filesToAdd];
    setImageFiles(newFiles);
    
    // 產生新圖片預覽
    const newPreviews = filesToAdd.map(file => URL.createObjectURL(file));
    setImagePreviews([...imagePreviews, ...newPreviews]);
    e.target.value = '';
  };

  const handleRemoveImage = idx => {
    const totalOriginalImages = originalImages.length;
    
    if (idx < totalOriginalImages) {
      const newOriginalImages = originalImages.filter((_, i) => i !== idx);
      const newImagePreviews = imagePreviews.filter((_, i) => i !== idx);
      
      setOriginalImages(newOriginalImages);
      setImagePreviews(newImagePreviews);
      
      console.log('移除原有圖片，剩餘原有圖片:', newOriginalImages);
    } else {
      const newImageIndex = idx - totalOriginalImages;
      const newImageFiles = imageFiles.filter((_, i) => i !== newImageIndex);
      const newImagePreviews = imagePreviews.filter((_, i) => i !== idx);
      
      setImageFiles(newImageFiles);
      setImagePreviews(newImagePreviews);
      
      console.log('移除新圖片，剩餘新圖片:', newImageFiles.length);
    }
  };

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });
  const handleSpecChange = (idx, field, value) => {
    const newSpecs = [...specs];
    if (field === "name") newSpecs[idx].name = value;
    setSpecs(newSpecs);
  };

  const handleOptionChange = (specIdx, optIdx, value) => {
    const newSpecs = [...specs];
    newSpecs[specIdx].options[optIdx] = value;
    setSpecs(newSpecs);
  };

  const addSpec = () => setSpecs([...specs, { name: "", options: [""] }]);
  
  const addOption = idx => {
    const newSpecs = [...specs];
    newSpecs[idx].options.push("");
    setSpecs(newSpecs);
  };

  const hasValidSpecs = useMemo(() =>
    specs.some(spec => spec.name.trim() && spec.options.some(opt => opt.trim())), [specs]);
    
  const skuCombinations = useMemo(() => {
    if (!hasValidSpecs) return [];
    const validSpecs = specs.filter(s => s.name.trim() && s.options.some(opt => opt.trim()));
    return cartesian(validSpecs);
  }, [specs, hasValidSpecs]);

  useEffect(() => {
    if (!hasValidSpecs) { 
      setSkuList([]); 
      return; 
    }
    
    setSkuList(prev => {
      const map = {};
      prev.forEach(sku => {
        const key = stableStringify(sku.spec);
        map[key] = sku;
      });
      return skuCombinations.map(specArr => {
        const spec = Object.assign({}, ...specArr);
        const key = stableStringify(spec);
        return map[key] || { spec, price: form.current_price || "", stock: form.stock_quantity || "" };
      });
    });
  }, [skuCombinations, hasValidSpecs]);

  const updateSkuField = (skuIndex, field, value) => {
    const newSkuList = [...skuList];
    newSkuList[skuIndex][field] = value;
    setSkuList(newSkuList);
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError("");
    
    try {
      if (!form.name || !form.category || !form.product_condition) {
        setError("請填寫必填欄位");
        return;
      }
      
      if (!imagePreviews.length) {
        setError("至少需要一張商品圖片");
        return;
      }

      if (hasValidSpecs && skuList.some(sku => sku.price === "" || sku.stock === "")) {
        setError("請為每個規格組合填寫價格與庫存");
        return;
      }

      if (!hasValidSpecs && (!form.current_price || !form.stock_quantity)) {
        setError("請填寫商品價格與庫存");
        return;
      }

      const formData = new FormData();
      formData.append("name", form.name);
      formData.append("category", form.category);
      formData.append("product_condition", form.product_condition);
      formData.append("description", form.description);
      formData.append("current_price", hasValidSpecs ? "" : form.current_price);
      formData.append("stock_quantity", hasValidSpecs ? "" : form.stock_quantity);
      formData.append("specifications", JSON.stringify(specs.filter(s => s.name && s.options.some(opt => opt))));
      
      if (hasValidSpecs) {
        const skusToSend = skuList.map(sku => ({
          spec: sku.spec || JSON.parse(sku.specJson),
          price: sku.price,
          stock: sku.stock
        }));
        formData.append("skus", JSON.stringify(skusToSend));
      }

      originalImages.forEach(imageUrl => {
        formData.append("keepImages", imageUrl);
      });

      imageFiles.forEach(file => {
        formData.append("images", file);
      });

      console.log('=== 提交資料 ===');
      console.log('要保留的圖片:', originalImages);
      console.log('新上傳的圖片數量:', imageFiles.length);
      console.log('總圖片數量:', imagePreviews.length);
      await api.put(`/api/products/${id}`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      
      alert("商品更新成功！");
      navigate("/my-seller");
    } catch (err) {
      console.error('更新失敗:', err);
      setError(err.response?.data?.message || "更新失敗，請檢查資料格式或必填欄位");
    }
  };

  if (loading) return <div className="loading">載入中...</div>;

  return (
    <form className="add-product-form" onSubmit={handleSubmit}>
      <div style={{ marginBottom: 20 }}>
        <label>商品圖片* ({imagePreviews.length}/8)</label>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          {imagePreviews.map((preview, idx) => (
            <div key={idx} style={{ position: "relative" }}>
              <img 
                src={preview} 
                alt={`預覽${idx + 1}`} 
                style={{ 
                  width: 120, 
                  height: 120, 
                  objectFit: "cover", 
                  borderRadius: 8,
                  border: "2px solid #e0e0e0"
                }} 
              />
              <div style={{
                position: "absolute", 
                bottom: 2, 
                left: 2, 
                background: idx < originalImages.length ? "#4CAF50" : "#2196F3", 
                color: "white", 
                padding: "2px 6px", 
                borderRadius: 4, 
                fontSize: 11,
                fontWeight: "bold"
              }}>
                {idx < originalImages.length ? "原有" : "新增"}
              </div>
              <button 
                type="button" 
                onClick={e => { 
                  e.stopPropagation(); 
                  handleRemoveImage(idx); 
                }} 
                style={{
                  position: "absolute", 
                  top: 4, 
                  right: 4, 
                  background: "rgba(255, 255, 255, 0.9)", 
                  border: "1px solid #ddd",
                  borderRadius: "50%", 
                  width: 24, 
                  height: 24, 
                  cursor: "pointer",
                  color: "#d23c3c", 
                  fontWeight: "bold", 
                  fontSize: 14,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center"
                }}
                title="移除圖片"
              >
                ✕
              </button>
            </div>
          ))}
          
          {imagePreviews.length < 8 && (
            <div 
              style={{
                width: 120, 
                height: 120, 
                border: "2px dashed #e0e0e0", 
                borderRadius: 8,
                background: "#fafafa", 
                display: "flex", 
                flexDirection: "column",
                alignItems: "center", 
                justifyContent: "center",
                cursor: "pointer",
                transition: "all 0.2s ease"
              }} 
              onClick={() => fileInputRef.current?.click()}
              onMouseEnter={e => e.target.style.borderColor = "#999"}
              onMouseLeave={e => e.target.style.borderColor = "#e0e0e0"}
            >
              <span style={{ color: "#999", fontSize: 24, marginBottom: 4 }}>+</span>
              <span style={{ color: "#999", fontSize: 11 }}>新增圖片</span>
              <input 
                type="file" 
                accept="image/*" 
                multiple 
                style={{ display: "none" }} 
                ref={fileInputRef} 
                onChange={handleImageChange} 
              />
            </div>
          )}
        </div>
        <div style={{ marginTop: 8, fontSize: 12, color: "#666" }}>
          支援 JPG、PNG 格式，最多8張圖片，建議尺寸 800x800 像素
        </div>
      </div>
      <div className="row">
        <label style={{ flex: 2 }}>
          商品名稱*
          <input 
            name="name" 
            value={form.name} 
            onChange={handleChange} 
            required 
            placeholder="請輸入商品名稱"
          />
        </label>
        <label style={{ flex: 1 }}>
          分類*
          <select name="category" value={form.category} onChange={handleChange} required>
            <option value="">請選擇</option>
            {CATEGORY_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
          </select>
        </label>
        <label style={{ flex: 1 }}>
          商品狀況*
          <select name="product_condition" value={form.product_condition} onChange={handleChange} required>
            <option value="">請選擇</option>
            {CONDITION_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
          </select>
        </label>
      </div>
      {!hasValidSpecs && (
        <div style={{ display: "flex", gap: 16 }}>
          <label style={{ flex: 1 }}>
            現價*
            <input 
              name="current_price" 
              type="number" 
              min="0"
              value={form.current_price} 
              onChange={handleChange} 
              required 
              placeholder="0"
            />
          </label>
          <label style={{ flex: 1 }}>
            庫存*
            <input 
              name="stock_quantity" 
              type="number" 
              min="0"
              value={form.stock_quantity} 
              onChange={handleChange} 
              required 
              placeholder="0"
            />
          </label>
        </div>
      )}
      <label>
        商品描述
        <textarea 
          name="description" 
          value={form.description} 
          onChange={handleChange} 
          style={{ width: "100%", minHeight: 100 }}
          placeholder="請輸入商品描述..."
        />
      </label>
      <div className="specs-section">
        <h4>商品規格</h4>
        {specs.map((spec, idx) => (
          <div key={idx} className="spec-block">
            <input 
              className="spec-name-input" 
              placeholder="規格名稱（如 顏色、尺寸）"
              value={spec.name}
              onChange={e => handleSpecChange(idx, "name", e.target.value)} 
            />
            <div className="spec-options-list">
              {spec.options.map((opt, oIdx) => (
                <input 
                  key={oIdx} 
                  className="spec-option-input" 
                  placeholder="選項"
                  value={opt}
                  onChange={e => handleOptionChange(idx, oIdx, e.target.value)} 
                />
              ))}
            </div>
            <button type="button" className="add-option-btn" onClick={() => addOption(idx)}>
              +選項
            </button>
          </div>
        ))}
        <button type="button" className="add-spec-btn" onClick={addSpec}>
          +新增規格
        </button>
      </div>

      {hasValidSpecs && skuCombinations.length > 0 && (
        <div style={{ marginTop: 24 }}>
          <h4>各規格組合價格與庫存</h4>
          <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: 16 }}>
            <thead>
              <tr style={{ background: "#f5f5f5" }}>
                {specs.filter(s => s.name.trim()).map(s => (
                  <th key={s.name} style={{ padding: 8, border: "1px solid #ddd" }}>{s.name}</th>
                ))}
                <th style={{ padding: 8, border: "1px solid #ddd" }}>價格</th>
                <th style={{ padding: 8, border: "1px solid #ddd" }}>庫存</th>
              </tr>
            </thead>
            <tbody>
              {skuList.map((sku, idx) => (
                <tr key={idx}>
                  {Object.values(sku.spec).map((value, i) => (
                    <td key={i} style={{ padding: 8, border: "1px solid #ddd", textAlign: "center" }}>
                      {value}
                    </td>
                  ))}
                  <td style={{ padding: 8, border: "1px solid #ddd" }}>
                    <input 
                      type="number" 
                      min="0" 
                      value={sku.price}
                      onChange={e => updateSkuField(idx, "price", e.target.value)}
                      required 
                      style={{ width: "80px", textAlign: "center" }} 
                    />
                  </td>
                  <td style={{ padding: 8, border: "1px solid #ddd" }}>
                    <input 
                      type="number" 
                      min="0" 
                      value={sku.stock}
                      onChange={e => updateSkuField(idx, "stock", e.target.value)}
                      required 
                      style={{ width: "80px", textAlign: "center" }} 
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {error && <div style={{ color: "#d23c3c", marginTop: 8, padding: 8, background: "#ffe6e6", borderRadius: 4 }}>{error}</div>}
      <div style={{ marginTop: 24, display: "flex", gap: 12 }}>
        <button 
          type="submit" 
          className="my-seller-add-btn" 
          style={{ flex: 1 }}
          disabled={loading}
        >
          {loading ? "更新中..." : "儲存修改"}
        </button>
        <button 
          type="button" 
          className="my-seller-add-btn" 
          style={{ flex: 1, background: "#6c757d" }} 
          onClick={() => navigate("/my-seller")}
        >
          取消
        </button>
      </div>
    </form>
  );
}
