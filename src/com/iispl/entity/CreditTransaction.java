package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CreditTransaction extends Transaction {

    private final long creditAccountId;

	
    public CreditTransaction(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, SourceSystem sourceSystem,
			long sourceSystemId, ChannelType channel, Bank fromBank, Bank toBank, BigDecimal amount,
			LocalDateTime txnDate, TransactionStatus status, long fromBankId, long toBankId, String settlementBatchId,
			long creditAccountId) {
		super(id, createdAt, updatedAt, sourceSystem, sourceSystemId, channel, fromBank, toBank, amount, txnDate,
				status, fromBankId, toBankId, settlementBatchId);
		this.creditAccountId = creditAccountId;
	}


	public Long getCreditAccountId() {
        return creditAccountId;
    }
}