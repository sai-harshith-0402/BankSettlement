package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class DebitTransaction extends Transaction {

    private final Long debitAccountId;

    public DebitTransaction(Long sourceSystemId, ChannelType channel,
                            Long fromBankId, Long toBankId,
                            BigDecimal amount, LocalDateTime txnDate,
                            Long debitAccountId) {
        super(sourceSystemId, channel, fromBankId, toBankId, amount, txnDate);
        this.debitAccountId = debitAccountId;
    }

    public Long getDebitAccountId() {
        return debitAccountId;
    }
}