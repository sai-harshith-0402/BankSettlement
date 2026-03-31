package com.iispl.entity;

import com.iispl.enums.ChannelType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Transaction extends BaseEntity {

    private Long sourceSystemId;
    private ChannelType channel;
    private Long fromBankId;
    private Long toBankId;
    private BigDecimal amount;
    private LocalDateTime txnDate;
    private TransactionStatus status;

    public Transaction(Long sourceSystemId, ChannelType channel,
                       Long fromBankId, Long toBankId,
                       BigDecimal amount, LocalDateTime txnDate) {
        this.sourceSystemId = sourceSystemId;
        this.channel = channel;
        this.fromBankId = fromBankId;
        this.toBankId = toBankId;
        this.amount = amount;
        this.txnDate = txnDate;
        this.status = TransactionStatus.INITIATED;
    }

    public Long getSourceSystemId() {
        return sourceSystemId;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public Long getFromBankId() {
        return fromBankId;
    }

    public Long getToBankId() {
        return toBankId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTxnDate() {
        return txnDate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}