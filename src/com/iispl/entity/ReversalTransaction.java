package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReversalTransaction extends Transaction {

    private final Long originalTransactionId;
    private final String reversalReason;


	public ReversalTransaction(SourceSystem sourceSystem, long sourceSystemId, ChannelType channel, Bank fromBank,
			Bank toBank, BigDecimal amount, LocalDateTime txnDate, TransactionStatus status, long fromBankId,
			long toBankId, long originalTransactionId, String reversalReason) {
		super(sourceSystem, sourceSystemId, channel, fromBank, toBank, amount, txnDate, status, fromBankId, toBankId);
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