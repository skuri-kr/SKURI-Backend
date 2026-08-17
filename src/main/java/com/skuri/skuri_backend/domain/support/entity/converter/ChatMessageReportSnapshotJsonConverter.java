package com.skuri.skuri_backend.domain.support.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatMessageReportSnapshotJsonConverter implements AttributeConverter<ChatMessageReportSnapshot, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;

    @Override
    public String convertToDatabaseColumn(ChatMessageReportSnapshot attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("채팅 신고 증거 스냅샷 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public ChatMessageReportSnapshot convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, ChatMessageReportSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("채팅 신고 증거 스냅샷 역직렬화에 실패했습니다.", e);
        }
    }
}
