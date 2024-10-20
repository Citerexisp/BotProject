package com.example.BotProject.controller;

import com.example.BotProject.Dictionary.ComponentInfoService;
import com.example.BotProject.entity.TelegramUser;
import com.example.BotProject.repository.TelegramUserRepository;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;



@Log4j
@Component
@PropertySource("classpath:application.properties")
public class MyBot extends TelegramLongPollingBot {

    private final Map<Long, List<String>> userComponents = new HashMap<>();
    private final Map<Long, Boolean> userInputState = new HashMap<>();

    private final ComponentInfoService componentInfoService= new ComponentInfoService();

    private final String botUsername;

    private final String botToken;
    private TelegramUserRepository repository;
    private Long chatId;


    public MyBot(@Value("${telegram.bot.username}") String botUsername,
                 @Value("${telegram.bot.token}") String botToken) {  // Изменили Repository на TelegramUserRepository
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.repository = repository;
    }


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override

    public void onUpdateReceived(Update update) {
        // Проверяем, что это текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            log.debug(messageText);

            if("/start".equalsIgnoreCase(messageText)) {
                sendWelcomeMessage(chatId);
            }

            else if ("Что я умею".equalsIgnoreCase(messageText)) {
                sendCapabilities(chatId);  // Отправляем клавиатуру с функциями бота
            } else if ("Анализировать состав".equalsIgnoreCase(messageText)) {
                startCompositionAnalysis(chatId);  // Начинаем процесс анализа состава
            } else if ("Рассказывает об отдельных компонентах".equalsIgnoreCase(messageText)) {
                sendMessage(chatId, "Введите название компонента, и я расскажу о нем. Пример ввода - /component глицерин");
            }
              else if(messageText.startsWith("/component ")){
                String componentName = messageText.substring(11).toLowerCase().trim();
                String response = componentInfoService.getComponentInfo(componentName);

                SendMessage message = new SendMessage(chatId.toString(), response);
                try {
                    execute(message);  // Отправка сообщения пользователю
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
              }
             else {
                // Обработка ввода компонентов в процессе анализа состава
                handleComponentInput(chatId, messageText);
            }
        }

        // Проверяем, что это нажатие на кнопку (CallbackQuery)
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            // Обработка нажатий на кнопки
            switch (callbackData) {
                case "analyze":
                    startCompositionAnalysis(chatId);
                    break;
                case "describe":
                    sendMessage(chatId, "Введите название компонента, и я расскажу о нем. Пример ввода - /component глицерин");
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда.");
                    break;
            }
        }
    }


    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());  // Преобразуем chatId в строку
        message.setText("Привет! Я бот, который поможет вам анализировать состав косметики.");

        // Создаем клавиатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);  // Клавиатура адаптируется под экран
        keyboardMarkup.setOneTimeKeyboard(true); // Клавиатура скроется после нажатия кнопки

        // Создаем строку с кнопками
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Что я умею"));

        // Создаем список строк и добавляем в клавиатуру
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        // Устанавливаем клавиатуру в сообщение
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: ", e);
        }
    }




    private void sendCapabilities(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());  // Преобразуем chatId в строку
        message.setText("Вот что я умею:");

        // Создаем инлайн клавиатуру
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        // Кнопка "Анализировать состав"
        row1.add(InlineKeyboardButton.builder()
                .text("Анализировать состав")
                .callbackData("analyze")
                .build());

        // Кнопка "Рассказывать об отдельных компонентах"
        row1.add(InlineKeyboardButton.builder()
                .text("Анализировать отдельные компоненты")
                .callbackData("describe")
                .build());

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: ", e);
        }
    }



    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());  // Преобразуем Long в String
        message.setText(text);

        try {
            execute(message);  // Отправляем сообщение
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: ", e);
        }
    }


    private void startCompositionAnalysis(Long chatId) {
        sendMessage(chatId, "Введите компоненты один за другим. Нажмите 'Отправить', когда закончите.");
        userInputState.put(chatId, true);  // Начинаем процесс ввода компонентов
        userComponents.put(chatId, new ArrayList<>());  // Инициализируем список компонентов для пользователя
    }

    private void handleComponentInput(Long chatId, String messageText) {
        // Если пользователь в режиме ввода компонентов
        if (userInputState.getOrDefault(chatId, false)) {
            // Если пользователь ввёл "Отправить", завершаем ввод компонентов
            if ("Отправить".equalsIgnoreCase(messageText)) {
                List<String> activeComponents = userComponents.getOrDefault(chatId, new ArrayList<>());

                // Объявляем StringBuilder для накопления информации о компонентах
                StringBuilder componentInfo = new StringBuilder();

                // Собираем информацию о всех компонентах
                for (String component : activeComponents) {
                    String info = componentInfoService.getComponentInfo(component.toLowerCase());
                    componentInfo.append(component).append(" - ").append(info).append("\n"); // Добавляем название и описание
                }

                sendMessage(chatId, "Активные компоненты:\n" + componentInfo.toString());

                // Сбрасываем состояние
                userInputState.put(chatId, false);
                userComponents.remove(chatId);
            }
             else {
                // Проверяем, содержится ли введённый компонент в списке components
                if (ComponentInfoService.components.containsKey(messageText.toLowerCase())) {
                    ArrayList<String> activeComponents = (ArrayList<String>) userComponents.computeIfAbsent(chatId, k -> new ArrayList<>());
                    activeComponents.add(messageText);
                    sendMessage(chatId, "Компонент добавлен. Введите следующий компонент или 'Отправить'.");
                }
            }





        } else {
            sendMessage(chatId, "Неизвестная команда.");
        }
    }



    //MySql

    private TelegramUser findOrSaveAppUser(Update update) {
        var telegramUser = update.getMessage().getFrom();

        // Ищем пользователя по Telegram ID, используя Optional для обработки случая null
        return Optional.ofNullable(repository.findByTelegramId(telegramUser.getId()))
                // Если пользователь не найден, создаем и сохраняем нового
                .orElseGet(() -> {
                    var newUser = TelegramUser.builder()
                            .telegramId(telegramUser.getId())
                            .username(telegramUser.getUserName())
                            .firstName(telegramUser.getFirstName())
                            .lastName(telegramUser.getLastName())
                            .build();
                    return repository.save(newUser);  // Сохраняем нового пользователя в репозиторий
                });
    }



    // Методы для обработки активных компонентов и информации о компонентах можно реализовать отдельно
}

