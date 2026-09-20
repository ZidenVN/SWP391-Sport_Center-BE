package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.repository.AccountRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionTest implements CommandLineRunner {
    private final AccountRepository accountRepository;
    @Override
    public void run(String... args) throws Exception {
        try {
            long count = accountRepository.count();
            log.info("==================================================");
            log.info("SUCCESS: Kết nối cơ sở dữ liệu thành công! Tổng số accounts hiện tại: {}", count);
            log.info("==================================================");
        } catch (Exception e) {
            log.error("==================================================");
            log.error("ERROR: Không thể kết nối đến cơ sở dữ liệu!", e);
            log.error("==================================================");
        }
    }
}
