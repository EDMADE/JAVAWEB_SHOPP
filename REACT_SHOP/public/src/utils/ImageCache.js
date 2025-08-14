class ImageCache {
  constructor() {
    this.cache = new Map();
    this.loadingPromises = new Map();
  }

  async loadImage(url) {
    if (this.cache.has(url)) {
      return this.cache.get(url);
    }

    if (this.loadingPromises.has(url)) {
      return this.loadingPromises.get(url);
    }

    const loadPromise = new Promise((resolve) => {
      const img = new Image();
      img.onload = () => {
        this.cache.set(url, url);
        this.loadingPromises.delete(url);
        resolve(url);
      };
      img.onerror = () => {
        const defaultUrl = '/images/default-product.png';
        this.cache.set(url, defaultUrl);
        this.loadingPromises.delete(url);
        resolve(defaultUrl);
      };
      img.src = url;
    });

    this.loadingPromises.set(url, loadPromise);
    return loadPromise;
  }

  preloadImage(url) {
    if (!this.cache.has(url) && !this.loadingPromises.has(url)) {
      this.loadImage(url);
    }
  }
}

export const imageCache = new ImageCache();
