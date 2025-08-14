import React, { useState, useRef } from "react";
import "./UploadImagePreview.css";

export default function UploadImagePreview({ onChange, initialUrl }) {
  const [fileSrc, setFileSrc] = useState(initialUrl || null);
  const inputRef = useRef();

  const handleUploadFile = (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      setFileSrc(reader.result);
      if (onChange) onChange(file, reader.result);
    };
    reader.readAsDataURL(file);
    e.target.value = "";
  };

  const handleClear = (e) => {
    e.preventDefault();
    setFileSrc(null);
    if (onChange) onChange(null, null);
  };

  return (
    <label className="upload-card">
      {fileSrc ? (
        <>
          <button className="clear-btn" onClick={handleClear}>刪除</button>
          <div className="upload-preview">
            <img src={fileSrc} alt="預覽" className="upload-preview-img" />
          </div>
        </>
      ) : (
        <span className="upload-card-button">點擊上傳圖片</span>
      )}
      <input
        type="file"
        accept="image/png, image/jpeg"
        onChange={handleUploadFile}
        ref={inputRef}
        className="upload-card-input"
      />
    </label>
  );
}
