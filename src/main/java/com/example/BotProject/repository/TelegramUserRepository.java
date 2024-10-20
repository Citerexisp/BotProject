package com.example.BotProject.repository;

import com.example.BotProject.entity.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.example.BotProject.repository")
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    TelegramUser findByTelegramId(Long telegramId);
}
