package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReversalTransaction extends Transaction {

    private final Long originalTransactionId;
    private final String reversalReason;

    public ReversalTransaction(Long sourceSystemId, ChannelType channel,
                               Long fromBankId, Long toBankId,
                               BigDecimal amount, LocalDateTime txnDate,
                               Long originalTransactionId,
                               String reversalReason) {
        super(sourceSystemId, channel, fromBankId, toBankId, amount, txnDate);
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