package com.fptu.swp391.sportscentermanager.repository;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

// Test query trên MySQL thật (cùng bản 8.4 với RDS): native query và hàm riêng của MySQL chạy đúng như production.
// Máy không có Docker thì các test này được bỏ qua, không làm fail build.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
abstract class MySqlRepositoryTest {

    // Singleton container: bật 1 lần cho cả JVM, không dùng @Container. Các class con dùng chung Spring context
    // (context được cache), nên nếu container bị tắt sau mỗi class thì class sau sẽ kết nối vào container đã chết.
    // Ryuk của Testcontainers tự xoá container khi JVM thoát.
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
        }
    }
}
