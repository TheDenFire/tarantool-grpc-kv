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
    private final String spaceName;
    private final int batchSize;

    public TarantoolKvRepository(TarantoolBoxClient client, String spaceName, int batchSize) {
        this.client = Objects.requireNonNull(client, "client");
        this.spaceName = Objects.requireNonNull(spaceName, "spaceName");
        this.batchSize = batchSize;
    }

    @Override
    public void put(String key, byte[] value) {
        try {
            client.eval(
                    "return box.space['" + spaceName + "']:replace({...})",
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
                    "return box.space['" + spaceName + "']:get({...})",
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
                    "return box.space['" + spaceName + "']:delete({...})",
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

        TarantoolBoxSpace space = client.space(spaceName);
        String lastSeenKey = null;
        while (true) {
            SelectResponse<List<Tuple<TarantoolKeyValueTuple>>> response;
            try {
                response = space.select(
                        List.of(lastSeenKey == null ? keySince : lastSeenKey),
                        SelectOptions.builder()
                                .withLimit(batchSize)
                                .withIterator(lastSeenKey == null ? BoxIterator.GE : BoxIterator.GT)
                                .build(),
                        TarantoolKeyValueTuple.class
                ).join();
            } catch (RuntimeException e) {
                throw new IllegalStateException("Tarantool operation failed", e.getCause() == null ? e : e.getCause());
            }

            List<Tuple<TarantoolKeyValueTuple>> tuples = response.get();
            if (tuples == null || tuples.isEmpty()) {
                return;
            }

            boolean reachedRangeEnd = false;
            for (Tuple<TarantoolKeyValueTuple> tuple : tuples) {
                TarantoolKeyValueTuple raw = tuple.get();
                if (raw == null || raw.getKey() == null) {
                    continue;
                }
                if (raw.getKey().compareTo(keyTo) >= 0) {
                    reachedRangeEnd = true;
                    break;
                }
                consumer.accept(raw.toDomain());
                lastSeenKey = raw.getKey();
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
                    "return box.space['" + spaceName + "']:count()",
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
