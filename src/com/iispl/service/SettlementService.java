package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.SettlementResult;

public interface SettlementService {

    /**
     * Settles all transactions in the given batch.
     * Updates NPCIBank balances for each transaction.
     * Produces a SettlementResult summarising the batch outcome.
     *
     * @param batch  the batch to settle
     * @return       SettlementResult with counts and total settled amount
     */
    SettlementResult settle(Batch batch);
}