package com.ej2.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * List<Integer>をJSON文字列に変換してデータベースに保存するコンバーター
 * 例: [1, 3, 5] -> "[1,3,5]"
 */
@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List<Integer>をJSON文字列に変換
     * @param attribute エンティティの属性値
     * @return データベース用のJSON文字列
     */
    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("リストをJSONに変換できません: " + attribute, e);
        }
    }

    /**
     * JSON文字列をList<Integer>に変換
     * @param dbData データベースのJSON文字列
     * @return エンティティ用のList<Integer>
     */
    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Integer>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("JSONをリストに変換できません: " + dbData, e);
        }
    }
}
