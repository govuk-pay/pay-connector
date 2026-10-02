package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.Event;
import uk.gov.pay.connector.events.model.charge.PaymentIncludedInPayout;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.gateway.adyen.AdyenPaymentProvider;
import uk.gov.pay.connector.gateway.adyen.report.AdyenBalancePayoutReportRecord;
import uk.gov.pay.connector.gateway.adyen.report.AdyenPayoutReportParser;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileHandler;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.CONNECT_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_PAYOUT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;

public class AdyenPayoutReconciliationHandler implements PayoutReconcileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenPayoutReconciliationHandler.class);

    private final AdyenPayoutReportParser adyenPayoutReportParser;
    private final AdyenPaymentProvider adyenPaymentProvider;
    private final EventService eventService;
    private final ConnectorConfiguration connectorConfiguration;

    @Inject
    public AdyenPayoutReconciliationHandler(PaymentProviders paymentProviders,
                                            AdyenPayoutReportParser adyenPayoutReportParser,
                                            EventService eventService,
                                            ConnectorConfiguration connectorConfiguration) {
        this.adyenPayoutReportParser = adyenPayoutReportParser;
        adyenPaymentProvider = (AdyenPaymentProvider) paymentProviders.byName(PaymentGatewayName.ADYEN);
        this.eventService = eventService;
        this.connectorConfiguration = connectorConfiguration;

    }

    public boolean reconcile(PayoutReconcileMessage payoutReconcileMessage) {
        MDC.put(GATEWAY_PAYOUT_ID, payoutReconcileMessage.getGatewayPayoutId());
        MDC.put(CONNECT_ACCOUNT_ID, payoutReconcileMessage.getConnectAccountId());

        AtomicInteger payments = new AtomicInteger();

        AdyenPayoutReconciliationPayload payout = (AdyenPayoutReconciliationPayload) payoutReconcileMessage.getPayout();
        try {
            String csv = adyenPaymentProvider.downloadReport(payout);
            
            var balancePayoutReportRecords = adyenPayoutReportParser.parse(csv);
            var payoutRecords = adyenPayoutReportParser.deduplicateRecords(balancePayoutReportRecords);

            Map<String, List<AdyenBalancePayoutReportRecord>> paidOutBalanceAccounts = balancePayoutReportRecords.stream()
                    .filter(r -> "bankTransfer".equals(r.type()))
                    .collect(Collectors.groupingBy(AdyenBalancePayoutReportRecord::balanceAccount));

            payoutRecords.forEach(record -> {
                switch (record.type()) {
                    case "capture":
                        emitPaymentEvent(record, paidOutBalanceAccounts.get(record.balanceAccount()).getFirst());
                        payments.getAndIncrement();
                        break;
                    case null:
                        LOGGER.error("Payout contains balance transfer of type null, which is unexpected.");
                        break;
                    default:
                        break;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to reconcile Adyen Payout report", e);
            return false;
        }
        return true;
    }

    private void emitPaymentEvent(AdyenBalancePayoutReportRecord payoutRecord, AdyenBalancePayoutReportRecord bankTransferRecord) {
        var paymentEvent = new PaymentIncludedInPayout(payoutRecord.pspPaymentMerchantReference(),
                bankTransferRecord.transferId(),
                bankTransferRecord.payoutDate());
        emitEvent(paymentEvent, payoutRecord.pspPaymentMerchantReference(), payoutRecord.transferId());

        LOGGER.info(format("Emitted event for payment [%s] included in payout [%s]",
                bankTransferRecord.pspPaymentMerchantReference(),
                kv(PAYMENT_EXTERNAL_ID, bankTransferRecord.pspPaymentMerchantReference())));
    }

    private void emitEvent(Event event, String transactionExternalId, String transferId) {
        if (TRUE.equals(connectorConfiguration.getEmitPayoutEvents())) {
            try {
                eventService.emitEvent(event,false);
            } catch (QueueException e) {
                throw new RuntimeException(format("Error sending %s event for transaction [%s] included in payout [%s] to event queue: %s",
                        event.getEventType(), transactionExternalId, transferId, e.getMessage()), e);
            }
        }
    }
}
