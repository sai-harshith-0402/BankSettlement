package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.SourceType;
import com.iispl.exception.AdapterException;
import com.iispl.ingestion.AdapterRegistry;
import com.iispl.ingestion.TransactionAdapter;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * IngestionWorker — Producer thread (Stage 1).
 *
 * One instance runs per SourceSystem in a fixed thread pool.
 * Responsibilities:
 *   1. Get the correct adapter from AdapterRegistry
 *   2. Call adapter.ingest(filePath) → List<IncomingTransaction>
 *   3. Push each IncomingTransaction into the shared transactionQueue
 *   4. Push N poison pills when done (one per SourceSystem count)
 *      so BatchProcessor knows all producers have finished
 *
 * ReentrantLock is NOT needed here — LinkedBlockingQueue is thread-safe
 * for concurrent producers. Multiple IngestionWorkers safely push to
 * the same queue without additional synchronization.
 */
public class IngestionWorker implements Runnable {

    private static final Logger logger = Logger.getLogger(IngestionWorker.class.getName());

    private final SourceSystem                          sourceSystem;
    private final AdapterRegistry                       adapterRegistry;
    private final LinkedBlockingQueue<IncomingTransaction> transactionQueue;
    private final int                                   totalSourceCount;

    public IngestionWorker(
            SourceSystem sourceSystem,
            AdapterRegistry adapterRegistry,
            LinkedBlockingQueue<IncomingTransaction> transactionQueue,
            int totalSourceCount) {

        this.sourceSystem      = sourceSystem;
        this.adapterRegistry   = adapterRegistry;
        this.transactionQueue  = transactionQueue;
        this.totalSourceCount  = totalSourceCount;
    }

    // =========================================================================
    // RUN
    // =========================================================================

    @Override
    public void run() {
        SourceType sourceType = sourceSystem.getSourceType();
        String     filePath   = sourceSystem.getFilePath();
        String     threadName = Thread.currentThread().getName();

        logger.info("[IngestionWorker:" + threadName + "] Starting | SourceType: "
                + sourceType + " | FilePath: " + filePath);

        try {
            // ── 1. Get adapter ─────────────────────────────────────────────────
            TransactionAdapter adapter = adapterRegistry.getAdapter(sourceType);

            // ── 2. Ingest all transactions from the file ───────────────────────
            List<IncomingTransaction> transactions = adapter.ingest(filePath);

            logger.info("[IngestionWorker:" + threadName + "] Ingested "
                    + transactions.size() + " transactions from " + sourceType);

            // ── 3. Push each transaction into the shared queue ─────────────────
            // BlockingQueue.put() blocks if queue is full — natural backpressure
            for (IncomingTransaction txn : transactions) {
                transactionQueue.put(txn);
            }

            logger.info("[IngestionWorker:" + threadName + "] All " + transactions.size()
                    + " transactions queued for " + sourceType);

        } catch (AdapterException e) {
            logger.severe("[IngestionWorker:" + threadName + "] Adapter failed for "
                    + sourceType + ": " + e.getMessage());

        } catch (InterruptedException e) {
            logger.severe("[IngestionWorker:" + threadName + "] Interrupted while queuing: "
                    + e.getMessage());
            Thread.currentThread().interrupt();

        } finally {
            // ── 4. Always push poison pill — even if ingestion failed ──────────
            // BatchProcessor counts one poison pill per SourceSystem
            try {
                transactionQueue.put(PipelineOrchestrator.POISON_TRANSACTION);
                logger.info("[IngestionWorker:" + threadName + "] Poison pill pushed for "
                        + sourceType);
            } catch (InterruptedException e) {
                logger.severe("[IngestionWorker:" + threadName
                        + "] Interrupted while pushing poison pill: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}