package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.SourceSystem;
import com.iispl.exception.AdapterException;

import java.util.List;

public interface BatchService {

    /**
     * Ingests transactions from a source system file, validates them,
     * groups by channelType, and returns one Batch per channel per date.
     *
     * @param sourceSystem  the source system whose filePath will be read
     * @return              list of Batches (one per channel found in the file)
     * @throws AdapterException if the file cannot be read or parsed
     */
    List<Batch> createBatches(SourceSystem sourceSystem) throws AdapterException;

    /**
     * Returns all batches created in the current run.
     */
    List<Batch> getAllBatches();
}