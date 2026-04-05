package ru.thedenfire.repository;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.client.box.options.SelectOptions;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.mapping.SelectResponse;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import ru.thedenfire.model.KeyValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class TarantoolKvRepository implements KvRepository {

    private final TarantoolBoxClient client;

    public TarantoolKvRepository(TarantoolBoxClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public void put(String key, byte[] value) {
        try {
            client.eval(
                    "return box.space.KV:replace({...})",
                    Arrays.asList(key, value)
            ).join();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public Optional<KeyValue> get(String key) {
        TarantoolResponse<List<?>> response;
        try {
            response = client.eval(
                    "return box.space.KV:get({...})",
                    List.of(key)
            ).join();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
        }

        List<?> result = response.get();
        if (result == null || result.isEmpty()) return Optional.empty();
        Object first = result.get(0);

        if (!(first instanceof List<?> tuple) || tuple.isEmpty()) return Optional.empty();
        String k = Objects.toString(tuple.get(0), null);

        if (k == null) return Optional.empty();
        byte[] v = tuple.size() > 1 ? (byte[]) tuple.get(1) : null;

        return Optional.of(new KeyValue(k, v));
    }

    @Override
    public boolean delete(String key) {
        TarantoolResponse<List<?>> response;
        try {
            response = client.eval(
                    "return box.space.KV:delete({...})",
                    List.of(key)
            ).join();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
        }
        List<?> tuple = response.get();
        return tuple != null && !tuple.isEmpty();
    }

    @Override
    public void scanRange(String keySince, String keyTo, Consumer<KeyValue> consumer) {
        Objects.requireNonNull(keySince, "keySince");
        Objects.requireNonNull(keyTo, "keyTo");
        Objects.requireNonNull(consumer, "consumer");

        if (keySince.compareTo(keyTo) >= 0) {
            return;
        }

        TarantoolBoxSpace space = client.space("KV");
        int batchSize = 1000;
        String lastSeenKey = null;
        while (true) {
            SelectResponse<List<Tuple<KeyValue>>> response;
            try {
                response = space.select(
                        List.of(lastSeenKey == null ? keySince : lastSeenKey),
                        SelectOptions.builder()
                                .withLimit(batchSize)
                                .withIterator(lastSeenKey == null ? BoxIterator.GE : BoxIterator.GT)
                                .build(),
                        KeyValue.class
                ).join();
            } catch (RuntimeException e) {
                throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
            }

            List<Tuple<KeyValue>> tuples = response.get();
            if (tuples == null || tuples.isEmpty()) {
                return;
            }

            boolean reachedRangeEnd = false;
            for (Tuple<KeyValue> tuple : tuples) {
                KeyValue keyValue = tuple.get();
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                if (keyValue.getKey().compareTo(keyTo) >= 0) {
                    reachedRangeEnd = true;
                    break;
                }
                consumer.accept(keyValue);
                lastSeenKey = keyValue.getKey();
            }

            if (reachedRangeEnd || tuples.size() < batchSize || lastSeenKey == null) {
                return;
            }
        }
    }

    @Override
    public long count() {
        TarantoolResponse<List<?>> response;
        try {
            response = client.eval(
                    "return box.space.KV:count()",
                    List.of()
            ).join();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
        }

        List<?> result = response.get();
        if (result == null || result.isEmpty()) return 0L;
        Object value = result.get(0);
        return value instanceof Number n ? n.longValue() : 0L;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close Tarantool client", e);
        }
    }
}
