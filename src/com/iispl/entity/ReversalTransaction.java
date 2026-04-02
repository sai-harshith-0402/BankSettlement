package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReversalTransaction extends Transaction {

    private final Long originalTransactionId;
    private final String reversalReason;


	

    public ReversalTransaction(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, SourceSystem sourceSystem,
			long sourceSystemId, ChannelType channel, Bank fromBank, Bank toBank, BigDecimal amount,
			LocalDateTime txnDate, TransactionStatus status, long fromBankId, long toBankId, String settlementBatchId,
			Long originalTransactionId, String reversalReason) {
		super(id, createdAt, updatedAt, sourceSystem, sourceSystemId, channel, fromBank, toBank, amount, txnDate,
				status, fromBankId, toBankId, settlementBatchId);
		this.originalTransactionId = originalTransactionId;
		this.reversalReason = reversalReason;
	}

	public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getReversalReason() {
        return reversalReason;
    }
}