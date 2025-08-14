import React, { useState, useRef, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import api from "../utils/api";
import Header from "../components/Header";
import Footer from "../components/Footer";
import "./AddProduct.css";

const CATEGORY_OPTIONS = [
  "電子產品", "服飾", "居家", "美妝", "書籍", "戶外", "其他"
];


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


export default function AddProduct({ onCartClick, cartCount = 0 }) {
  const { user, isLoggedIn } = useAuth();
  const navigate = useNavigate();

  
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);
  const fileInputRef = useRef();

  const [isAuction, setIsAuction] = useState(false);

  const [form, setForm] = useState({
    name: "",
    category: "",
    product_condition: "",
    description: "",
    start_price: "",
    current_price: "",
    stock_quantity: "",
    bid_end_time: "",
  });

  const [specs, setSpecs] = useState([{ name: "", options: [""] }]);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  
  const hasValidSpecs = useMemo(() => {
    return specs.some(spec => 
      spec.name.trim() && spec.options.some(opt => opt.trim())
    );
  }, [specs]);

  
  const skuCombinations = useMemo(() => {
    if (!hasValidSpecs) return [];
    const validSpecs = specs.filter(s => s.name.trim() && s.options.some(opt => opt.trim()));
    return cartesian(validSpecs);
  }, [specs, hasValidSpecs]);

 
  const [skuList, setSkuList] = useState([]);

  
  React.useEffect(() => {
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

  // 圖片處理
  const handleImageChange = e => {
    const files = Array.from(e.target.files);
    if (!files.length) return;
    const newFiles = [...imageFiles, ...files].slice(0, 8);
    setImageFiles(newFiles);
    const newPreviews = newFiles.map(file => URL.createObjectURL(file));
    setImagePreviews(newPreviews);
  };

  const handleRemoveImage = idx => {
    const newFiles = imageFiles.filter((_, i) => i !== idx);
    const newPreviews = imagePreviews.filter((_, i) => i !== idx);
    setImageFiles(newFiles);
    setImagePreviews(newPreviews);
  };

  // 表單變更
  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  
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

  
  const updateSkuField = (skuIndex, field, value) => {
    const newSkuList = [...skuList];
    newSkuList[skuIndex][field] = value;
    setSkuList(newSkuList);
  };

  // 競標切換
  const handleAuctionToggle = () => {
  setIsAuction((prev) => !prev);
 
  setForm(f => ({
    ...f,
    current_price: f.current_price || "",
    start_price: "",
    bid_end_time: "",
    stock_quantity: "",
  }));
};

  
  const handleSubmit = async e => {
    e.preventDefault();
    setError("");
    setSubmitting(true);

    try {
     
      if (!form.name || !form.category || !form.product_condition) {
        setError("請填寫必填欄位");
        setSubmitting(false);
        return;
      }
       if (hasValidSpecs) {
      if (skuList.some(sku => !sku.price || !sku.stock)) {
        setError("請為每個規格組合填寫價格與數量");
        setSubmitting(false);
        return;
      }
    } else {
      if (!form.current_price || !form.stock_quantity) {
        setError("請填寫商品價格與數量");
        setSubmitting(false);
        return;
      }
    }

      if (!imageFiles.length) {
        setError("至少上傳一張商品圖片");
        setSubmitting(false);
        return;
      }


      
if (isAuction) {
  if (!form.start_price || !form.bid_end_time) {
    setError("競標商品需填寫起標價與截止時間");
    setSubmitting(false);
    return;
  }
} else if (hasValidSpecs) {
  if (skuList.some(sku => !sku.price || !sku.stock)) {
    setError("請為每個規格組合填寫價格與數量");
    setSubmitting(false);
    return;
  }
} else {
  if (!form.current_price || !form.stock_quantity) {
    setError("請填寫商品價格與數量");
    setSubmitting(false);
    return;
  }
}


      if (isAuction && (!form.start_price || !form.bid_end_time)) {
        setError("競標商品需填寫起標價與截止時間");
        setSubmitting(false);
        return;
      }

      const formData = new FormData();
      formData.append("seller_id", user.userId);
      formData.append("name", form.name);
      formData.append("category", form.category);
      formData.append("product_condition", form.product_condition);
      formData.append("description", form.description);
      formData.append("start_price", isAuction ? form.start_price : "");
      formData.append("current_price", hasValidSpecs ? "" : form.current_price);
      formData.append("stock_quantity", hasValidSpecs ? "" : form.stock_quantity);
      formData.append("bid_end_time", isAuction ? form.bid_end_time : "");
      formData.append("specifications", JSON.stringify(specs.filter(s => s.name && s.options.some(opt => opt))));
      formData.append("status", isAuction ? "AUCTION" : "ACTIVE");
      
      
      if (hasValidSpecs) {
        formData.append("skus", JSON.stringify(skuList));
      }

      imageFiles.forEach(file => formData.append("images", file));

      await api.post("/api/products", formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });

      alert("商品上架成功！");
      navigate("/my-seller");
    } catch (err) {
      setError("上架失敗，請檢查資料格式或必填欄位");
    } finally {
      setSubmitting(false);
    }
  };

  if (!isLoggedIn) return <div className="my-seller-hint">請先登入</div>;

  return (
    <div className="my-seller-bg">
      <Header onCartClick={onCartClick} cartCount={cartCount} />
      <main className="my-seller-main" style={{ width: "100%", maxWidth: 1200, margin: "0 auto" }}>
        <h2 className="my-seller-title">新增商品</h2>
        <form className="add-product-form" onSubmit={handleSubmit}>
          
          
          <div style={{ marginBottom: 20 }}>
            <label style={{ fontWeight: 500, marginBottom: 8, display: "block" }}>
              商品圖片 <span style={{ color: "#d23c3c" }}>*</span>
            </label>
            <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
              {imagePreviews.map((preview, idx) => (
                <div key={idx} style={{ position: "relative" }}>
                  <img
                    src={preview}
                    alt={`預覽${idx + 1}`}
                    style={{ width: 120, height: 120, objectFit: "contain", borderRadius: 8 }}
                  />
                  <button
                    type="button"
                    onClick={e => { e.stopPropagation(); handleRemoveImage(idx); }}
                    style={{
                      position: "absolute", top: 2, right: 2, background: "#fff", border: "none",
                      borderRadius: "50%", width: 22, height: 22, cursor: "pointer", 
                      color: "#d23c3c", fontWeight: "bold", fontSize: 16
                    }}
                    title="移除圖片"
                  >✕</button>
                </div>
              ))}
              <div
                style={{
                  width: 120, height: 120, border: "2px dashed #e0e0e0", borderRadius: 8,
                  background: "#fafafa", display: "flex", alignItems: "center", justifyContent: "center",
                  cursor: "pointer"
                }}
                onClick={() => fileInputRef.current?.click()}
              >
                <span style={{ color: "#bbb", fontSize: 18 }}>+</span>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  style={{ display: "none" }}
                  ref={fileInputRef}
                  onChange={handleImageChange}
                />
              </div>
            </div>
          </div>

          
          <div className="row">
            <label style={{ flex: 2 }}>
              商品名稱<span style={{ color: "#d23c3c" }}>*</span>
              <input name="name" value={form.name} onChange={handleChange} required />
            </label>
            <label style={{ flex: 1 }}>
              分類<span style={{ color: "#d23c3c" }}>*</span>
              <select name="category" value={form.category} onChange={handleChange} required>
                <option value="">請選擇</option>
                {CATEGORY_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
              </select>
            </label>
            <label style={{ flex: 1 }}>
             商品狀況<span style={{ color: "#d23c3c" }}>*</span>
      <select name="product_condition" value={form.product_condition} onChange={handleChange}>
        <option value="">請選擇</option>
        <option value="全新">全新</option>
        <option value="二手">二手</option>
      </select>
    </label>
          </div>

       
         <div style={{ margin: "12px 0" }}>
  <button
    type="button"
    className={`auction-toggle-btn ${isAuction ? "on" : "off"}`}
    onClick={() => {
      setIsAuction(v => !v);
      setForm(f => ({
        ...f,
        start_price: "",
        bid_end_time: "",
        current_price: "",
        stock_quantity: "",
      }));
    }}
    style={{
      padding: "7px 20px",
      borderRadius: 20,
      border: "none",
      fontWeight: "bold",
      cursor: "pointer",
      background: isAuction ? "#ff6600" : "#eee",
      color: isAuction ? "#fff" : "#666",
      marginRight: 8
    }}
  >
    {isAuction ? "已開啟競標" : "開啟競標"}
  </button>
  <span style={{ color: "#888", fontSize: 13 }}>
    {isAuction ? "（此商品將以競標方式販售）" : "（可設定競標商品）"}
  </span>
</div>

      

{!isAuction && !hasValidSpecs && (
  <div style={{ display: "flex", gap: 16 }}>
    <label style={{ flex: 1 }}>
      價格<span style={{ color: "#d23c3c" }}>*</span>
      <input
        name="current_price"
        type="number"
        value={form.current_price}
        onChange={handleChange}
        required
        placeholder="請輸入現價"
      />
    </label>
    <label style={{ flex: 1 }}>
      商品數量<span style={{ color: "#d23c3c" }}>*</span>
      <input
        name="stock_quantity"
        type="number"
        value={form.stock_quantity}
        onChange={handleChange}
        required
        placeholder="請輸入數量"
      />
    </label>
  </div>
)}
 

{isAuction && (
  <div style={{ display: "flex", gap: 16, marginBottom: 8 }}>
    <label style={{ flex: 1 }}>
      起標價<span style={{ color: "#d23c3c" }}>*</span>
      <input
        name="start_price"
        type="number"
        value={form.start_price}
        onChange={handleChange}
        required
        placeholder="請輸入起標價"
      />
    </label>
    <label style={{ flex: 1 }}>
      直購價（可選填）
      <input
        name="current_price"
        type="number"
        value={form.current_price}
        onChange={handleChange}
        placeholder="可選填"
      />
    </label>
    <label style={{ flex: 1 }}>
      商品數量<span style={{ color: "#d23c3c" }}>*</span>
      <input
        name="stock_quantity"
        type="number"
        value={form.stock_quantity}
        onChange={handleChange}
        required
        placeholder="請輸入數量"
      />
    </label>
    <label style={{ flex: 1 }}>
      競標截止時間<span style={{ color: "#d23c3c" }}>*</span>
      <input
        name="bid_end_time"
        type="datetime-local"
        value={form.bid_end_time}
        onChange={handleChange}
        required
        min={new Date(Date.now() + 60000).toISOString().slice(0, 16)}
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
              style={{ width: "100%" }} 
            />
          </label>

        
          {!isAuction && (
          <div className="specs-section">
  {specs.map((spec, idx) => (
    <div key={idx} className="spec-block">
      <input
        className="spec-name-input"
        placeholder="規格名稱（如 顏色）"
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
            style={{ width: 120, minWidth: 60, marginRight: 8, marginBottom: 6 }}
          />
        ))}
        <button
          type="button"
          className="add-option-btn"
          onClick={() => addOption(idx)}
          style={{ height: 36, padding: "0 12px", marginBottom: 6 }}
        >
          +選項
        </button>
      </div>
    </div>
  ))}
  <button type="button" className="add-spec-btn" onClick={addSpec}>
    +新增規格
  </button>
</div>
    )}

       
          {!isAuction && hasValidSpecs && skuCombinations.length > 0 && (
            <div style={{ marginTop: 24 }}>
              <h4>各規格組合價格與數量</h4>
              <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: 16 }}>
                <thead>
                  <tr style={{ background: "#f5f5f5" }}>
                    {specs.filter(s => s.name.trim()).map(s => (
                      <th key={s.name} style={{ padding: 8, border: "1px solid #ddd" }}>
                        {s.name}
                      </th>
                    ))}
                    <th style={{ padding: 8, border: "1px solid #ddd" }}>價格</th>
                    <th style={{ padding: 8, border: "1px solid #ddd" }}>數量</th>
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

          {error && <div style={{ color: "#d23c3c", marginTop: 8 }}>{error}</div>}
          
          <div style={{ marginTop: 16, display: "flex", gap: 12 }}>
            <button 
              type="submit" 
              className="my-seller-add-btn" 
              disabled={submitting} 
              style={{ flex: 1 }}
            >
              {submitting ? "上架中..." : "上架商品"}
            </button>
            <button 
              type="button" 
              className="my-seller-add-btn" 
              style={{ flex: 1, background: "#ccc" }} 
              onClick={() => navigate("/my-seller")}
            >
              取消
            </button>
          </div>
        </form>
      </main>
      <Footer />
    </div>
  );
}
