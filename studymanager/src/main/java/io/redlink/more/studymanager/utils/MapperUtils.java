package io.redlink.more.studymanager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.redlink.more.studymanager.exception.BadRequestException;

public class MapperUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T readValue(Object o, Class<T> c) {
        if(o == null) return null;
        try {
            return MAPPER.readValue(o.toString(), c);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(e);
        }
    }

    public static String writeValueAsString(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(e);
        }
    }
}
