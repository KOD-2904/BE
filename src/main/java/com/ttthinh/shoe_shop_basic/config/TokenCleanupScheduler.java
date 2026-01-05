//package com.ttthinh.shoe_shop_basic.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
//@Component
//public class TokenCleanupScheduler {
//
//    @Autowired
//    private InvalidatedTokenRepository invalidatedTokenRepository;
//
//    // Chạy mỗi ngày lúc 2h sáng
//    @Scheduled(cron = "0 0 2 * * ?")
//    public void cleanupExpiredTokens() {
//        try {
//            Date now = new Date();
//            invalidatedTokenRepository.deleteExpiredTokens(now);
//            System.out.println("Đã cleanup token hết hạn");
//        } catch (Exception e) {
//            System.out.println("Cleanup failed: " + e.getMessage());
//        }
//    }
//}
