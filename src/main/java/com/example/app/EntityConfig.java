package com.example.app;

import java.util.List;

public record EntityConfig(
        String name,
        String listPath,
        String createPath,
        String updatePathTemplate,
        String deletePathTemplate,
        String idField,
        List<FieldConfig> columns,
        List<FieldConfig> formFields
) {
    public String updatePath(Object id) {
        return updatePathTemplate.replace("{id}", String.valueOf(id));
    }

    public String deletePath(Object id) {
        return deletePathTemplate.replace("{id}", String.valueOf(id));
    }
}
