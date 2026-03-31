package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class InterBankTransaction extends Transaction {

    private final Long nostroAccountId;

    public InterBankTransaction(Long sourceSystemId, ChannelType channel,
                                Long fromBankId, Long toBankId,
                                BigDecimal amount, LocalDateTime txnDate,
                                Long nostroAccountId) {
        super(sourceSystemId, channel, fromBankId, toBankId, amount, txnDate);
        this.nostroAccountId = nostroAccountId;
    }

    public Long getNostroAccountId() {
        return nostroAccountId;
    }
}