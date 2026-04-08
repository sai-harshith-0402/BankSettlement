package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.NettingPosition;
import com.iispl.entity.ReconciliationEntry;

import java.util.List;

public interface ReconciliationService {

    /**
     * Reconciles the settled batch against the computed netting positions.
     *
     * For each bank in the netting positions:
     *   - expectedAmount = netAmount from NettingPosition
     *   - actualAmount   = current NPCIBank.balanceAmount change after settlement
     *   - variance       = actualAmount - expectedAmount
     *   - reconStatus    = MATCHED / PARTIALLY_MATCHED / UNMATCHED / EXCEPTION
     *
     * @param batch            the settled batch
     * @param nettingPositions netting positions computed for this batch
     * @return                 list of ReconciliationEntries, one per bank
     */
    List<ReconciliationEntry> reconcile(Batch batch, List<NettingPosition> nettingPositions);
}