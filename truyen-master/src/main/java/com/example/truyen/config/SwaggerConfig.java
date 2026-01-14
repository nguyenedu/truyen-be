package com.example.truyen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("🎭 Web Truyện Online API")
                        .version("1.0.0")
                        .description("""
                            API Documentation cho hệ thống quản lý truyện online.
                            
                            ## Tính năng chính:
                            - 🔐 Xác thực JWT (Bearer Token)
                            - 👥 Quản lý người dùng (USER, ADMIN, SUPER_ADMIN)
                            - 📚 Quản lý truyện và chương
                            - 💬 Bình luận và đánh giá
                            - ⭐ Yêu thích và lịch sử đọc
                            
                            ## Cách sử dụng:
                            1. Đăng nhập qua `/api/auth/login`
                            2. Copy token từ response
                            3. Click nút "Authorize" ở trên
                            4. Nhập: `Bearer {your_token}`
                            5. Gọi các API bảo mật
                            """)
                        .contact(new Contact()
                                .name("Truyen Team")
                                .email("support@webtruyen.com")
                                .url("https://github.com/yourusername/truyen"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("🖥️ Local Development Server"),
                        new Server()
                                .url("https://api.webtruyen.com")
                                .description("🌐 Production Server")))

                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                            Nhập JWT token (không cần thêm từ 'Bearer').
                                            
                                            VD: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                                            
                                            Lấy token từ response của API /api/auth/login
                                            """)));
    }
}