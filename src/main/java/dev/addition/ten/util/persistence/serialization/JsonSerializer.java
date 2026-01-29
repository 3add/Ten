package dev.addition.ten.util.persistence.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class JsonSerializer implements Serializer {

    private static final Gson GSON = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .create();
    private static final JsonSerializer instance = new JsonSerializer();

    public static JsonSerializer getInstance() {
        return instance;
    }

    @Override
    public <T> String serialize(T object) {
        return GSON.toJson(object);
    }

    @Override
    public <T> T deserialize(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public <T> T deserialize(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    public <T> T deserialize(String json, TypeToken<T> typeToken) {
        return GSON.fromJson(json, typeToken.getType());
    }
}