package com.iispl.threading;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.service.BatchService;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * PRODUCER
 *
 * One IngestionWorker is created per active SourceSystem.
 * It reads and adapts all rows from the source's .xlsx file
 * (via BatchService) and puts each IncomingTransaction onto
 * the shared BlockingQueue for consumers to pick up.
 *
 * Thread lifecycle:
 *   PipelineOrchestrator submits this to the producer thread pool.
 *   run() completes when all rows for this source are enqueued.
 */
public class IngestionWorker implements Runnable {

    private final SourceSystem sourceSystem;
    private final BatchService batchService;
    private final BlockingQueue<IncomingTransaction> queue;

    public IngestionWorker(SourceSystem sourceSystem,
                           BatchService batchService,
                           BlockingQueue<IncomingTransaction> queue) {
        this.sourceSystem = sourceSystem;
        this.batchService = batchService;
        this.queue        = queue;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] IngestionWorker started for source: "
                + sourceSystem.getSystemCode());

        try {
            List<IncomingTransaction> transactions = batchService.readAndAdapt(sourceSystem);

            for (IncomingTransaction txn : transactions) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                queue.put(txn);   // blocks if queue is full — back-pressure
                System.out.println("[" + threadName + "] Queued txn from "
                        + sourceSystem.getSystemCode()
                        + " | queue size: " + queue.size());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // restore interrupt flag
            System.err.println("[" + threadName + "] IngestionWorker interrupted for source: "
                    + sourceSystem.getSystemCode());
        } catch (Exception e) {
            System.err.println("[" + threadName + "] IngestionWorker failed for source: "
                    + sourceSystem.getSystemCode() + " | reason: " + e.getMessage());
        }

        System.out.println("[" + threadName + "] IngestionWorker finished for source: "
                + sourceSystem.getSystemCode());
    }
}