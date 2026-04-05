package ru.thedenfire.repository;

import ru.thedenfire.model.KeyValue;

import java.util.Optional;
import java.util.function.Consumer;

public interface KvRepository extends AutoCloseable {
    void put(String key, String value);
    Optional<KeyValue> get(String key);
    boolean delete(String key);
    void scanRange(String keySince, String keyTo, Consumer<KeyValue> consumer);
    long count();

    @Override
    void close();
}
