package ru.thedenfire.service;

import ru.thedenfire.model.KeyValue;
import ru.thedenfire.repository.KvRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class KvServiceImpl implements KvService {

    private final KvRepository repository;

    public KvServiceImpl(KvRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void put(String key, String value) {
        repository.put(key, value);
    }

    @Override
    public Optional<KeyValue> get(String key) {
        return repository.get(key);
    }

    @Override
    public boolean delete(String key) {
        return repository.delete(key);
    }

    @Override
    public void scanRange(String keySince, String keyTo, Consumer<KeyValue> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        repository.scanRange(keySince, keyTo, consumer);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void close() {
        repository.close();
    }
}
