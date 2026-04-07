package com.iispl.ingestion;

import java.util.HashMap;
import java.util.Map;

import com.iispl.enums.SourceType;
import com.iispl.exception.AdapterException;

public class AdapterRegistry {

    // =========================================================================
    // REGISTRY MAP
    // =========================================================================

    private final Map<SourceType, TransactionAdapter> registry = new HashMap<>();

    // =========================================================================
    // CONSTRUCTOR — register all adapters at startup
    // =========================================================================

    public AdapterRegistry() {
        register(new CbsAdapter());
        register(new SwiftAdapter());
        register(new NeftUpiAdapter());
        register(new RtgsAdapter());
        register(new FintechAdapter());
    }

    // =========================================================================
    // REGISTER
    // =========================================================================

    private void register(TransactionAdapter adapter) {
        SourceType type = SourceType.valueOf(adapter.getSourceSystemType());
        registry.put(type, adapter);
    }

    // =========================================================================
    // GET ADAPTER
    // =========================================================================

    /**
     * Returns the correct adapter for a given SourceSystemType.
     * Throws AdapterException if no adapter is registered for that type.
     *
     * @param sourceSystemType  the type read from the SourceSystem DB record
     * @return                  the matching TransactionAdapter
     * @throws AdapterException if the type is not registered
     */
    public TransactionAdapter getAdapter(SourceType sourceSystemType) throws AdapterException {
        TransactionAdapter adapter = registry.get(sourceSystemType);
        if (adapter == null) {
            throw new AdapterException(
                "No adapter registered for source system type: " + sourceSystemType
            );
        }
        return adapter;
    }
}