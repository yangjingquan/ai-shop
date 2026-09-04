package com.shop.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 兼容后端约定格式和前端常见 ISO LocalDateTime 格式。
 * 输出格式仍由 JacksonConfig 统一为 yyyy-MM-dd HH:mm:ss。
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
        }
        String text = parser.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, SPACE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException exception) {
                return (LocalDateTime) context.handleWeirdStringValue(
                        LocalDateTime.class, text, "时间格式应为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss");
            }
        }
    }
}
