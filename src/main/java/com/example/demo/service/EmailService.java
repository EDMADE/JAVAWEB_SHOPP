// src/main/java/com/example/demo/service/EmailService.java
package com.example.demo.service;

import com.example.demo.model.entity.User;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.Order;
import com.example.demo.model.dto.OrderItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.List;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    public void sendVerificationEmail(String toEmail, String username, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("競好購 - 請驗證您的Email");
            helper.setFrom(fromEmail, "競好購");
            
            String verificationUrl = frontendUrl + "/verify-email/" + verificationToken;
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>歡迎加入競好購！</h2>
                    <p>親愛的 %s，</p>
                    <p>請點擊下方連結驗證您的Email：</p>
                    <a href="%s" style="background: #007bff; color: white; padding: 10px 20px; text-decoration: none;">
                        驗證Email
                    </a>
                    <p>此連結將在30分鐘過期。</p>
                </div>
                """, username, verificationUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 驗證郵件已發送至: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("❌ 發送驗證郵件失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("發送驗證郵件失敗", e);
        }
    }
    

    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🎉 歡迎加入競好購大家庭！");
            helper.setFrom(fromEmail, "競好購");
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">🎉 歡迎加入！</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">歡迎 %s！</h2>
                        <p style="color: #555; line-height: 1.6;">
                            恭喜您成功加入競好購！您現在可以盡情享受我們的服務：
                        </p>
                        
                        <div style="margin: 30px 0;">
                            <p style="margin: 15px 0;"><span style="font-size: 20px;">🔨</span> 參與刺激的商品競標</p>
                            <p style="margin: 15px 0;"><span style="font-size: 20px;">🛒</span> 安全便利的購物體驗</p>
                            <p style="margin: 15px 0;"><span style="font-size: 20px;">❤️</span> 收藏和追蹤心儀商品</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                                      color: white; padding: 14px 30px; text-decoration: none; 
                                      border-radius: 6px; display: inline-block; font-weight: bold;">
                                🚀 開始探索
                            </a>
                        </div>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                        <p style="color: #666; font-size: 12px; margin: 0; text-align: center;">
                            感謝您選擇競好購！<br>
                            © 2025 競好購團隊
                        </p>
                    </div>
                </div>
                """, username, frontendUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 歡迎郵件已發送至: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("❌ 發送歡迎郵件失敗: " + e.getMessage());
        }
    }
    
    //發送賣家新訂單通知
    public void sendNewOrderNotification(User seller, User buyer, Product product, 
                                       Integer quantity, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(seller.getEmail());
            helper.setSubject("🛒 競好購 - 您有新訂單需要處理");
            helper.setFrom(fromEmail, "競好購");
            
            String orderUrl = frontendUrl + "/seller/orders/" + order.getOrderId();
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #28a745 0%%, #20c997 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">🛒 您有新訂單！</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">新訂單通知</h2>
                        <p style="color: #555; line-height: 1.6;">親愛的賣家 <strong>%s</strong>，</p>
                        <p style="color: #555; line-height: 1.6;">您的商品收到新訂單，請盡快安排出貨：</p>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">📋 訂單資訊</h3>
                            <p><strong>訂單編號：</strong>ORD%08d</p>
                            <p><strong>商品名稱：</strong>%s</p>
                            <p><strong>購買數量：</strong>%d 件</p>
                            <p><strong>買家：</strong>%s</p>
                            <p><strong>訂單時間：</strong>%s</p>
                            <p><strong>單價：</strong>NT$ %s</p>
                            <p><strong>小計：</strong><span style="color: #28a745; font-weight: bold;">NT$ %s</span></p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: #28a745; color: white; padding: 14px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block; font-weight: bold;">
                                📦 查看訂單詳情
                            </a>
                        </div>
                        
                        <div style="background: #fff3cd; border: 1px solid #ffeeba; padding: 15px; border-radius: 6px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                💡 <strong>溫馨提醒：</strong>請盡快處理訂單並安排出貨，提供優質的購物體驗。
                            </p>
                        </div>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                        <p style="color: #666; font-size: 12px; margin: 0; text-align: center;">
                            感謝您在競好購平台上銷售商品！<br>
                            © 2025 競好購團隊
                        </p>
                    </div>
                </div>
                """, 
                seller.getUsername(),
                order.getOrderId(),
                product.getName(),
                quantity,
                buyer.getUsername(),
                order.getCreatedAt(),
                product.getCurrentPrice(),
                product.getCurrentPrice().multiply(BigDecimal.valueOf(quantity)),
                orderUrl
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 賣家新訂單通知已發送至: " + seller.getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ 發送賣家通知失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("發送賣家通知失敗", e);
        }
    }
    
    //發送買家訂單確認信
    public void sendOrderConfirmation(User buyer, Order order, List<OrderItemDTO> orderItems) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(buyer.getEmail());
            helper.setSubject("✅ 競好購 - 訂單確認通知");
            helper.setFrom(fromEmail, "競好購");
            
            String orderUrl = frontendUrl + "/my-orders/" + order.getOrderId();
            
            
            StringBuilder itemsHtml = new StringBuilder();
            for (OrderItemDTO item : orderItems) {
                itemsHtml.append(String.format(
                    "<li style='margin: 10px 0; padding: 15px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #007bff;'>" +
                    "<div style='display: flex; justify-content: space-between; align-items: center;'>" +
                    "<div>" +
                    "<strong>商品ID:</strong> %d<br>" +
                    "<strong>數量:</strong> %d 件<br>" +
                    "<strong>單價:</strong> NT$ %s" +
                    "</div>" +
                    "<div style='text-align: right;'>" +
                    "<strong style='color: #007bff;'>小計: NT$ %s</strong>" +
                    "</div>" +
                    "</div>" +
                    "</li>",
                    item.getProductId(), 
                    item.getQuantity(), 
                    item.getPrice(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ));
            }
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #007bff 0%%, #0056b3 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">✅ 訂單確認成功！</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">感謝您的購買！</h2>
                        <p style="color: #555; line-height: 1.6;">親愛的 <strong>%s</strong>，</p>
                        <p style="color: #555; line-height: 1.6;">感謝您的購買！您的訂單已確認，我們會盡快為您安排出貨。</p>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">📋 訂單資訊</h3>
                            <p><strong>訂單編號：</strong>ORD%08d</p>
                            <p><strong>訂單時間：</strong>%s</p>
                            <p><strong>總金額：</strong><span style="color: #e74c3c; font-size: 18px; font-weight: bold;">NT$ %s</span></p>
                            
                            <h4 style="color: #333; margin-top: 20px;">📍 收件人資訊</h4>
                            <div style="background: white; padding: 15px; border-radius: 6px; border: 1px solid #dee2e6;">
                                <p style="margin: 5px 0;"><strong>姓名：</strong>%s</p>
                                <p style="margin: 5px 0;"><strong>電話：</strong>%s</p>
                                <p style="margin: 5px 0;"><strong>地址：</strong>%s</p>
                                %s
                            </div>
                            
                            <h4 style="color: #333; margin-top: 20px;">🛍️ 購買商品</h4>
                            <ul style="list-style: none; padding: 0; margin: 0;">%s</ul>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: #007bff; color: white; padding: 14px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block; font-weight: bold;">
                                📱 查看訂單狀態
                            </a>
                        </div>
                        
                        <div style="background: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 6px;">
                            <p style="margin: 0; color: #155724; font-size: 14px;">
                                📦 <strong>出貨通知：</strong>我們會在商品出貨時通知您，請保持聯絡方式暢通。
                            </p>
                        </div>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                        <p style="color: #666; font-size: 12px; margin: 0; text-align: center;">
                            感謝您選擇競好購！期待您的下次光臨。<br>
                            © 2025 競好購團隊
                        </p>
                    </div>
                </div>
                """, 
                buyer.getUsername(),
                order.getOrderId(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                (order.getNote() != null && !order.getNote().trim().isEmpty()) 
                    ? String.format("<p style='margin: 5px 0;'><strong>備註：</strong>%s</p>", order.getNote()) 
                    : "",
                itemsHtml.toString(),
                orderUrl
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 買家訂單確認信已發送至: " + buyer.getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ 發送買家確認信失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("發送買家確認信失敗", e);
        }
    }
    
    //發送出貨通知（賣家出貨後通知買家）
    public void sendShippingNotification(User buyer, Order order, String trackingNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(buyer.getEmail());
            helper.setSubject("📦 競好購 - 您的訂單已出貨");
            helper.setFrom(fromEmail, "競好購");
            
            String orderUrl = frontendUrl + "/my-orders/" + order.getOrderId();
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #ff6b35 0%%, #f7931e 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">📦 您的訂單已出貨！</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">訂單出貨通知</h2>
                        <p style="color: #555; line-height: 1.6;">親愛的 <strong>%s</strong>，</p>
                        <p style="color: #555; line-height: 1.6;">您的訂單已經出貨，正在配送中！</p>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">📋 出貨資訊</h3>
                            <p><strong>訂單編號：</strong>ORD%08d</p>
                            <p><strong>出貨時間：</strong>%s</p>
                            %s
                            <p><strong>收件地址：</strong>%s</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: #ff6b35; color: white; padding: 14px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block; font-weight: bold;">
                                🔍 追蹤訂單狀態
                            </a>
                        </div>
                        
                        <div style="background: #fff3cd; border: 1px solid #ffeeba; padding: 15px; border-radius: 6px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                📞 <strong>收貨提醒：</strong>請保持聯絡方式暢通，配送員會在送達前聯繫您。
                            </p>
                        </div>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                        <p style="color: #666; font-size: 12px; margin: 0; text-align: center;">
                            感謝您使用競好購！<br>
                            © 2025 競好購團隊
                        </p>
                    </div>
                </div>
                """, 
                buyer.getUsername(),
                order.getOrderId(),
                java.time.LocalDateTime.now(),
                (trackingNumber != null && !trackingNumber.trim().isEmpty()) 
                    ? String.format("<p><strong>物流追蹤號：</strong>%s</p>", trackingNumber) 
                    : "",
                order.getReceiverAddress(),
                orderUrl
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 出貨通知已發送至: " + buyer.getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ 發送出貨通知失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("發送出貨通知失敗", e);
        }
    }
    
    
    public void sendWinningNotification(User winner, Product product, BigDecimal winningAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(winner.getEmail());
            helper.setSubject("🎉 恭喜您得標！- " + product.getName());
            helper.setFrom(fromEmail, "競好購");
            
            String productUrl = frontendUrl + "/product/" + product.getProductId();
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #00d2ff 0%%, #3a7bd5 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">🎉 恭喜得標！</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">恭喜您在競標中獲勝！</h2>
                        <p style="color: #555; line-height: 1.6;">親愛的 <strong>%s</strong>，</p>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">得標商品資訊</h3>
                            <p><strong>商品名稱：</strong>%s</p>
                            <p><strong>得標金額：</strong><span style="color: #e74c3c; font-size: 18px; font-weight: bold;">NT$ %s</span></p>
                            <p><strong>商品編號：</strong>PROD%06d</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: linear-gradient(135deg, #00d2ff 0%%, #3a7bd5 100%%); 
                                      color: white; padding: 14px 30px; text-decoration: none; 
                                      border-radius: 6px; display: inline-block; font-weight: bold;">
                                💳 前往付款
                            </a>
                        </div>
                        
                        <div style="background: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 6px;">
                            <p style="margin: 0; color: #155724; font-size: 14px;">
                                📅 請於 <strong>3日內</strong> 完成付款，逾期將取消訂單。
                            </p>
                        </div>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                        <p style="color: #666; font-size: 12px; margin: 0; text-align: center;">
                            感謝您使用競好購！<br>
                            © 2025 競好購團隊
                        </p>
                    </div>
                </div>
                """, 
                winner.getUsername(), 
                product.getName(), 
                winningAmount.toString(), 
                product.getProductId(), 
                productUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 得標通知已發送給: " + winner.getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ 發送得標通知失敗: " + e.getMessage());
        }
    }


    public void sendAuctionEndReminder(User user, Product product) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject("⏰ 競標即將結束 - " + product.getName());
            helper.setFrom(fromEmail, "競好購");
            
            String productUrl = frontendUrl + "/product/" + product.getProductId();
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #ff9a56 0%%, #ffad56 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">⏰ 競標提醒</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">競好購</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #333; margin-top: 0;">競標即將結束！</h2>
                        <p style="color: #555; line-height: 1.6;">親愛的 <strong>%s</strong>，</p>
                        <p style="color: #555; line-height: 1.6;">您關注的商品競標即將在 <strong>15分鐘內</strong> 結束：</p>
                        
                        <div style="background: #fff3cd; border: 1px solid #ffeaa7; padding: 20px; border-radius: 6px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">商品資訊</h3>
                            <p><strong>商品名稱：</strong>%s</p>
                            <p><strong>當前價格：</strong><span style="color: #e74c3c; font-size: 18px; font-weight: bold;">NT$ %s</span></p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: linear-gradient(135deg, #ff9a56 0%%, #ffad56 100%%); 
                                      color: white; padding: 14px 30px; text-decoration: none; 
                                      border-radius: 6px; display: inline-block; font-weight: bold;">
                                🔨 立即前往競標
                            </a>
                        </div>
                    </div>
                </div>
                """, 
                user.getUsername(), 
                product.getName(), 
                product.getCurrentPrice().toString(), 
                productUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ 競標結束提醒已發送給: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ 發送競標結束提醒失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("發送競標結束提醒失敗", e);
        }
    }

   

    }

