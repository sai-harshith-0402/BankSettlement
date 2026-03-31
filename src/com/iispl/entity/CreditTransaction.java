package com.iispl.entity;

import com.iispl.enums.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CreditTransaction extends Transaction {

    private final Long creditAccountId;

    public CreditTransaction(Long sourceSystemId, ChannelType channel,
                             Long fromBankId, Long toBankId,
                             BigDecimal amount, LocalDateTime txnDate,
                             Long creditAccountId) {
        super(sourceSystemId, channel, fromBankId, toBankId, amount, txnDate);
        this.creditAccountId = creditAccountId;
    }

    public Long getCreditAccountId() {
        return creditAccountId;
    }
}