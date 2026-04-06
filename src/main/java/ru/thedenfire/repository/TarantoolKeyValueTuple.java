package ru.thedenfire.repository;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.thedenfire.model.KeyValue;

@NoArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"key", "value"})
class TarantoolKeyValueTuple {
    private String key;
    private byte[] value;

    KeyValue toDomain() {
        return new KeyValue(key, value);
    }
}
