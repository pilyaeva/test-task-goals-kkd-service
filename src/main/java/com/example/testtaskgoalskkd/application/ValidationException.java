package com.example.testtaskgoalskkd.application;

/**
 * Некорректные входные данные запроса. Сообщение предназначено для клиента API.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
