import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-fallback">
          <h2>出現錯誤</h2>
          <p>頁面載入時發生問題，請重新整理頁面或回到首頁。</p>
          <button onClick={() => window.location.href = '/'}>
            回到首頁
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
