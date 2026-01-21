package com.ej2.converter;

import com.ej2.model.DaySchedule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * List<DaySchedule>をJSON文字列に変換してデータベースに保存するコンバーター
 * 例: [{day:1, periodStart:1, periodEnd:3}] -> "[{\"day\":1,\"periodStart\":1,\"periodEnd\":3}]"
 */
@Converter
public class DayScheduleListConverter implements AttributeConverter<List<DaySchedule>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List<DaySchedule>をJSON文字列に変換
     * @param attribute エンティティの属性値
     * @return データベース用のJSON文字列
     */
    @Override
    public String convertToDatabaseColumn(List<DaySchedule> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("DayScheduleリストをJSONに変換できません: " + attribute, e);
        }
    }

    /**
     * JSON文字列をList<DaySchedule>に変換
     * @param dbData データベースのJSON文字列
     * @return エンティティ用のList<DaySchedule>
     */
    @Override
    public List<DaySchedule> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<List<DaySchedule>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("JSONをDayScheduleリストに変換できません: " + dbData, e);
        }
    }
}
