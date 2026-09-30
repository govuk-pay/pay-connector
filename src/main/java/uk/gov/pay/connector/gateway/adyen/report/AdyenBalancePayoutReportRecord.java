package uk.gov.pay.connector.gateway.adyen.report;

import java.math.BigDecimal;
import java.time.Instant;

public record AdyenBalancePayoutReportRecord(String balanceAccount,
                                             String transferId,
                                             String transactionId,
                                             String category,
                                             String type,
                                             String status,
                                             BigDecimal balance,
                                             String reference,
                                             String description,
                                             String pspPaymentMerchantReference,
                                             String pspModificationMerchantReference,
                                             Instant payoutDate) {
}
