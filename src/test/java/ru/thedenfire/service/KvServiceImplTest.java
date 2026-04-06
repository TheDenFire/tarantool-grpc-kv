package ru.thedenfire.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.thedenfire.model.KeyValue;
import ru.thedenfire.repository.KvRepository;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KvServiceImplTest {

    @Mock
    KvRepository repository;

    @InjectMocks
    KvServiceImpl service;

    @Test
    void constructor_nullRepository_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new KvServiceImpl(null))
                .withMessageContaining("repository");
    }

    @Test
    void put_delegatesToRepository() {
        byte[] value = "hello".getBytes();
        service.put("key1", value);

        verify(repository).put("key1", value);
    }

    @Test
    void put_withNullValue_delegatesToRepository() {
        service.put("key1", null);

        verify(repository).put("key1", null);
    }

    @Test
    void get_found_returnsResult() {
        KeyValue kv = new KeyValue("key1", "val1".getBytes());
        when(repository.get("key1")).thenReturn(Optional.of(kv));

        Optional<KeyValue> result = service.get("key1");

        assertThat(result).isPresent().contains(kv);
        verify(repository).get("key1");
    }

    @Test
    void get_notFound_returnsEmpty() {
        when(repository.get("missing")).thenReturn(Optional.empty());

        assertThat(service.get("missing")).isEmpty();
    }

    @Test
    void delete_existing_returnsTrue() {
        when(repository.delete("key1")).thenReturn(true);

        assertThat(service.delete("key1")).isTrue();
        verify(repository).delete("key1");
    }

    @Test
    void delete_nonExistent_returnsFalse() {
        when(repository.delete("missing")).thenReturn(false);

        assertThat(service.delete("missing")).isFalse();
    }

    @Test
    void scanRange_delegatesToRepository() {
        Consumer<KeyValue> consumer = kv -> {};
        service.scanRange("a", "z", consumer);

        verify(repository).scanRange("a", "z", consumer);
    }

    @Test
    void scanRange_nullConsumer_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> service.scanRange("a", "z", null))
                .withMessageContaining("consumer");
    }

    @Test
    void count_delegatesToRepository() {
        when(repository.count()).thenReturn(42L);

        assertThat(service.count()).isEqualTo(42L);
        verify(repository).count();
    }

    @Test
    void close_delegatesToRepository() {
        service.close();

        verify(repository).close();
    }
}
