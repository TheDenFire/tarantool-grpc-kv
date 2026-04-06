package ru.thedenfire.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KeyValue {
    private final String key;
    private final byte[] value;
}
