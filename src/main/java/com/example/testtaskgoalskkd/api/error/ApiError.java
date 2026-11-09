package com.example.testtaskgoalskkd.api.error;

/**
 * Единый формат ошибки API (см. specs/design.md, «Обработка ошибок»).
 */
public record ApiError(String code, String message) {
}
