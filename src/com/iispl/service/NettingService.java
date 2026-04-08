package com.iispl.service;

import com.iispl.entity.Batch;
import com.iispl.entity.NettingPosition;

import java.util.List;

public interface NettingService {

    /**
     * Computes multilateral netting positions for all CREDIT and DEBIT
     * transactions in the given batch.
     *
     * Groups transactions by counterparty bank name, computes gross debit,
     * gross credit, and net amount for each bank pair.
     *
     * @param batch  the settled batch to net
     * @return       list of NettingPositions (one per unique counterparty bank)
     */
    List<NettingPosition> computeNetting(Batch batch);
}