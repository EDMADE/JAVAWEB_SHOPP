import React, { useState } from 'react';
import axios from 'axios';

export default function Login({ onLogin }) {
  const [form, setForm] = useState({ username: '', password: '' });
  const [message, setMessage] = useState('');

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    try {
      const res = await axios.post('http://localhost:8080/api/auth/signin', form);
      localStorage.setItem('jwtToken', res.data.token); 
      onLogin(res.data); 
      setMessage('登入成功！');
    } catch (err) {
      setMessage('登入失敗：' + (err.response?.data?.message || '帳號或密碼錯誤'));
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="username" value={form.username} onChange={handleChange} placeholder="使用者名稱" required />
      <input name="password" type="password" value={form.password} onChange={handleChange} placeholder="密碼" required />
      <button type="submit">登入/註冊</button>
      {message && <div>{message}</div>}
    </form>
  );
}
