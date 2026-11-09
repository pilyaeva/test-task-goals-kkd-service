package com.example.testtaskgoalskkd.api.error;

/**
 * Единый формат ошибки API: {@code {"error": {"code": "...", "message": "..."}}}
 * (см. specs/design.md, «Обработка ошибок»).
 */
public record ApiError(Body error) {

    public ApiError(String code, String message) {
        this(new Body(code, message));
    }

    public record Body(String code, String message) {
    }
}
