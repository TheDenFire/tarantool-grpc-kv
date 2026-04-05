package ru.thedenfire.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"key", "value"})
public class KeyValue {
    private String key;
    private byte[] value;
}
