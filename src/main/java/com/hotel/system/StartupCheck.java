package com.hotel.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class StartupCheck implements CommandLineRunner {

    private final DataSource dataSource;

    public StartupCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== KIỂM TRA CẤU HÌNH HỆ THỐNG ===");
        try (Connection conn = dataSource.getConnection()) {
            System.out.println(" Database: Kết nối PostgreSQL thành công!");
        } catch (Exception e) {
            System.out.println("Database: Kết nối thất bại.");
            System.out.println("Lý do: " + e.getMessage());
        }
        System.out.println("==================================\n");
    }
}