import React, { useState } from "react";

export default function FavoriteImage({ src, alt, ...rest }) {
  const [loading, setLoading] = useState(true);

  return (
    <div style={{ position: "relative", width: 120, height: 120 }}>
      {loading && (
        <div
          style={{
            position: "absolute",
            width: "100%",
            height: "100%",
            background: "#f3f3f3",
            borderRadius: 8,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1
          }}
        >
          <span style={{ color: "#aaa", fontSize: 14 }}>圖片載入中...</span>
        </div>
      )}
      <img
        src={src}
        alt={alt}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
          borderRadius: 8,
          display: loading ? "none" : "block"
        }}
        onLoad={() => setLoading(false)}
        onError={e => {
          e.target.src = "/default-product.jpg";
          setLoading(false);
        }}
        {...rest}
      />
    </div>
  );
}
