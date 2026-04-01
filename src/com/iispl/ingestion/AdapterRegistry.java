package com.iispl.ingestion;

import com.iispl.enums.SourceType;

import java.util.HashMap;
import java.util.Map;

public class AdapterRegistry {

    private final Map<SourceType, TransactionAdapter> registry = new HashMap<>();

    public void register(SourceType sourceType, TransactionAdapter adapter) {
        registry.put(sourceType, adapter);
    }

    public TransactionAdapter getAdapter(SourceType sourceType) {
        TransactionAdapter adapter = registry.get(sourceType);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "No adapter registered for source type: " + sourceType);
        }
        return adapter;
    }

    public boolean hasAdapter(SourceType sourceType) {
        return registry.containsKey(sourceType);
    }
}