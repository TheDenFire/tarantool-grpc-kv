package ru.thedenfire.repository;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import ru.thedenfire.model.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TarantoolKvRepositoryTest {

    @Container
    static final GenericContainer<?> tarantool = new GenericContainer<>("tarantool/tarantool:3.2.3")
            .withExposedPorts(3301)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("tarantool/init.lua"),
                    "/opt/tarantool/init.lua"
            )
            .withCommand("tarantool", "/opt/tarantool/init.lua")
            .withEnv("TARANTOOL_LISTEN", "0.0.0.0:3301")
            .withEnv("KV_SPACE_NAME", "KV")
            .waitingFor(Wait.forListeningPort());

    static TarantoolBoxClient client;
    static KvRepository repository;

    @BeforeAll
    static void setup() throws Exception {
        client = TarantoolFactory.box()
                .withHost(tarantool.getHost())
                .withPort(tarantool.getMappedPort(3301))
                .withUser("guest")
                .build();
        repository = new TarantoolKvRepository(client);
    }

    @AfterAll
    static void teardown() throws Exception {
        repository.close();
    }

    @BeforeEach
    void cleanup() {
        client.eval("box.space.KV:truncate()", List.of()).join();
    }

    // ── put / get ────────────────────────────────────────────────────────────

    @Test
    void put_get_returnsStoredValue() {
        repository.put("key1", "hello".getBytes());

        Optional<KeyValue> result = repository.get("key1");

        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("key1");
        assertThat(new String(result.get().getValue()))
                .isEqualTo("hello");
    }

    @Test
    void put_withNullValue_storesNull() {
        repository.put("key1", null);

        Optional<KeyValue> result = repository.get("key1");

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isNull();
    }

    @Test
    void put_overwritesExistingKey() {
        repository.put("1", "first".getBytes());
        repository.put("1", "second".getBytes());

        Optional<KeyValue> result = repository.get("1");

        assertThat(result).isPresent();

        assertThat(repository.get("1"))
                .get()
                .extracting(KeyValue::getValue)
                .isEqualTo("second".getBytes());
    }

    @Test
    void get_nonExistentKey_returnsEmpty() {
        assertThat(repository.get("missing")).isEmpty();
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_existingKey_returnsTrue() {
        repository.put("key1", "value1".getBytes());

        assertThat(repository.delete("key1")).isTrue();
    }

    @Test
    void delete_existingKey_removesKey() {
        repository.put("key1", "value1".getBytes());
        repository.delete("key1");

        assertThat(repository.get("key1")).isEmpty();
    }

    @Test
    void delete_nonExistentKey_returnsFalse() {
        assertThat(repository.delete("missing")).isFalse();
    }

    // ── count ────────────────────────────────────────────────────────────────

    @Test
    void count_emptySpace_returnsZero() {
        assertThat(repository.count()).isZero();
    }

    @Test
    void count_afterInserts_returnsCorrectCount() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.put("c", "3".getBytes());

        assertThat(repository.count()).isEqualTo(3L);
    }

    @Test
    void count_afterDelete_decrements() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.delete("a");

        assertThat(repository.count()).isEqualTo(1L);
    }

    // ── scanRange ────────────────────────────────────────────────────────────

    @Test
    void scanRange_returnsKeysInRange() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.put("c", "3".getBytes());
        repository.put("d", "4".getBytes());

        List<KeyValue> results = new ArrayList<>();
        repository.scanRange("b", "d", results::add);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(KeyValue::getKey).containsExactly("b", "c");
    }

    @Test
    void scanRange_exclusiveEnd_excludesLastKey() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.put("c", "3".getBytes());

        List<KeyValue> results = new ArrayList<>();
        repository.scanRange("a", "c", results::add);

        assertThat(results).extracting(KeyValue::getKey).containsExactly("a", "b");
    }

    @Test
    void scanRange_inclusiveStart_includesFirstKey() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());

        List<KeyValue> results = new ArrayList<>();
        repository.scanRange("a", "z", results::add);

        assertThat(results).extracting(KeyValue::getKey).contains("a");
    }

    @Test
    void scanRange_noMatchingKeys_returnsEmpty() {
        repository.put("a", "1".getBytes());
        repository.put("z", "2".getBytes());

        List<KeyValue> results = new ArrayList<>();
        repository.scanRange("m", "n", results::add);

        assertThat(results).isEmpty();
    }

    @Test
    void scanRange_returnsValues() {
        repository.put("k1", "val1".getBytes());
        repository.put("k2", "val2".getBytes());

        List<KeyValue> results = new ArrayList<>();
        repository.scanRange("k1", "k3", results::add);

        assertThat(results).hasSize(2);
        assertThat(new String(results.get(0).getValue()))
                .isEqualTo("val1");

        assertThat(new String(results.get(1).getValue()))
                .isEqualTo("val2");
    }
}
