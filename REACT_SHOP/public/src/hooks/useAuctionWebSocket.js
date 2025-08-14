import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export const useAuctionWebSocket = (productId, onBidUpdate) => {
  const stompClient = useRef(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!productId) return;

    const socket = new SockJS('http://localhost:8080/ws-auction');
    stompClient.current = Stomp.over(socket);
    
    stompClient.current.connect({}, 
      (frame) => {
        console.log('✅ WebSocket 連接成功: ' + frame);
        setConnected(true);
        
        stompClient.current.subscribe(`/topic/auction/${productId}`, (message) => {
          const bidData = JSON.parse(message.body);
          console.log('📢 收到新出價:', bidData);
          if (onBidUpdate) {
            onBidUpdate(bidData);
          }
        });
      },
      (error) => {
        console.error('❌ WebSocket 連接失敗:', error);
        setConnected(false);
      }
    );

    return () => {
      if (stompClient.current) {
        stompClient.current.disconnect();
        setConnected(false);
      }
    };
  }, [productId, onBidUpdate]);

  const sendBid = (amount) => {
    if (stompClient.current && connected) {
      stompClient.current.send(`/app/bid/${productId}`, {}, JSON.stringify({
        amount: amount
      }));
    }
  };

  return { connected, sendBid };
};
