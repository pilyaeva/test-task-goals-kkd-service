package com.example.testtaskgoalskkd.application;

/**
 * Запрошенный ресурс не найден. Сообщение предназначено для клиента API.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
