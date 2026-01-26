package dev.addition.randomkits.util.persistence.serialization;

public interface Serializer {
    <T> String serialize(T object);
    <T> T deserialize(String json, Class<T> clazz);
}