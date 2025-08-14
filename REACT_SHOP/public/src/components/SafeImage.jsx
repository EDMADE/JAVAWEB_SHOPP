import React, { useState, useEffect } from "react";
const API_BASE_URL = "http://localhost:8080";

export default function SafeImage({ src, alt, className, ...rest }) {
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(false);
  
  let realSrc = src;
  if (src && src.startsWith("/")) {
    realSrc = API_BASE_URL + src;
  }
  
  useEffect(() => {
    setLoaded(false);
    setError(false);
  }, [realSrc]);
  
  return (
    <div style={{ position: "relative", width: 60, height: 60 }}>
      {!loaded && !error && (
        <div style={{
          position: "absolute", width: "100%", height: "100%",
          background: "linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%)",
          backgroundSize: "200% 100%",
          animation: "loading 1.5s infinite",
          borderRadius: 8,
          display: "flex", alignItems: "center", justifyContent: "center"
        }}>
          <span style={{ color: "#999", fontSize: 12 }}>載入中...</span>
        </div>
      )}
      <img
        src={realSrc || "/default-product.jpg"}
        alt={alt}
        className={className}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
          borderRadius: 8,
          display: loaded || error ? "block" : "none"
        }}
        onLoad={() => {
          setLoaded(true);
          setError(false);
        }}
        onError={e => {
          console.warn(`圖片載入失敗: ${realSrc}, fallback to default`);
          e.target.onerror = null;
          e.target.src = "/default-product.jpg";
          setError(true);
          setLoaded(true);
        }}
        {...rest}
      />
    </div>
  );
}
