package com.example.app;

public record FieldConfig(
        String key,
        String label,
        FieldType type,
        boolean required
) {
}
