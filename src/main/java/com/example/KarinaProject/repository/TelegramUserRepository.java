package com.example.KarinaProject.repository;

import com.example.KarinaProject.entity.TelegramUser;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.example.KarinaProject.repository")
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    TelegramUser findByTelegramId(Long telegramId);
}
