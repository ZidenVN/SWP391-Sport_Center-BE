package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionTest implements CommandLineRunner {
    private final UserRepository userRepository;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            long count = userRepository.count();
            log.info("==================================================");
            log.info("SUCCESS: Kết nối cơ sở dữ liệu thành công! Tổng số users hiện tại: {}", count);
            log.info("==================================================");
        } catch (Exception e) {
            log.error("==================================================");
            log.error("ERROR: Không thể kết nối đến cơ sở dữ liệu!", e);
            log.error("==================================================");
        }
    }
}
