# Sports Center Manager - Backend

# Hướng dẫn cài đặt và chạy dự án
1. **Cấu hình MySQL:**
   - Tạo database rỗng tên là `sportcenter_db` trong MySQL của bạn.
   - Kiểm tra file `src/main/resources/application.properties`, đảm bảo `username` và `password` khớp với máy bạn (Mặc định trong code đang để mật khẩu là `mypasswordisnull`).
2. **Chạy ứng dụng:**
   - Mở Terminal, gõ lệnh: `./mvnw clean spring-boot:run`
   - Nhờ có file `DataInitializer.java`, khi chạy lần đầu, hệ thống sẽ **tự động seed (bơm)** toàn bộ các Quyền (Permission), Vai trò (Role) và tạo sẵn 1 tài khoản Admin mặc định (`admin@sportcenter.com` / `toilaadmin`).
  
