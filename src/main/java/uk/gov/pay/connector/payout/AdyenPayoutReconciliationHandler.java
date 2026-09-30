package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.Event;
import uk.gov.pay.connector.events.model.charge.PaymentIncludedInPayout;
import uk.gov.pay.connector.events.model.payout.PayoutPaid;
import uk.gov.pay.connector.events.model.refund.RefundIncludedInPayout;
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
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

public class AdyenPayoutReconciliationHandler implements PayoutReconcileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenPayoutReconciliationHandler.class);

    private final AdyenPayoutReportParser adyenPayoutReportParser;
    private final AdyenPaymentProvider adyenPaymentProvider;

    private final ConnectorConfiguration connectorConfiguration;
    private final EventService eventService;
    private final PayoutEmitterService payoutEmitterService;
    private static final Set<String> RELEVANT_TYPES = Set.of("bankTransfer", "capture", "refund");

    @Inject
    public AdyenPayoutReconciliationHandler(PaymentProviders paymentProviders,
                                            AdyenPayoutReportParser adyenPayoutReportParser,
                                            ConnectorConfiguration connectorConfiguration,
                                            EventService eventService,
                                            PayoutEmitterService payoutEmitterService) {
        this.adyenPayoutReportParser = adyenPayoutReportParser;
        adyenPaymentProvider = (AdyenPaymentProvider) paymentProviders.byName(PaymentGatewayName.ADYEN);
        this.connectorConfiguration = connectorConfiguration;
        this.eventService = eventService;
        this.payoutEmitterService = payoutEmitterService;
    }

    public boolean reconcile(PayoutReconcileMessage payoutReconcileMessage) {

        AdyenPayoutReconciliationPayload payout = (AdyenPayoutReconciliationPayload) payoutReconcileMessage.getPayout();

        try {
            LOGGER.atInfo()
                    .setMessage("Started reconciling Adyen balance platform payout report")
                    .addKeyValue("file_name", payout.getFileName())
                    .addKeyValue("report_type", payout.getReportType())
                    .addKeyValue("report_date", payout.getCreationDate())
                    .addKeyValue("environment", payout.getEnvironment())
                    .log();

            String csv = adyenPaymentProvider.downloadReport(payout);
            List<AdyenBalancePayoutReportRecord> records = adyenPayoutReportParser.parse(csv);
            reconcileRecords(records);

            LOGGER.atInfo()
                    .setMessage("Finished reconciling Adyen balance platform payout report")
                    .log();
        } catch (Exception e) {
            LOGGER.error("Failed to reconcile Adyen balance platform payout report", e);
            return false;
        }
        return true;
    }

    private void reconcileRecords(List<AdyenBalancePayoutReportRecord> payoutReportRecords) {

        Map<String, List<AdyenBalancePayoutReportRecord>> paidOutBalanceAccounts = payoutReportRecords.stream()
                .filter(r -> "bankTransfer".equals(r.type()))
                .collect(Collectors.groupingBy(AdyenBalancePayoutReportRecord::balanceAccount));


        payoutReportRecords
                .forEach(payoutRecord -> {
                    switch (payoutRecord.type()) {
                        case "capture":
                            emitPaymentEvent(payoutRecord, paidOutBalanceAccounts.get(payoutRecord.balanceAccount()).getFirst());
                            break;
                        case "refund":
                            emitRefundEvent(payoutRecord, paidOutBalanceAccounts.get(payoutRecord.balanceAccount()).getFirst());
                            break;
                        case "bankTransfer":
                            emitPayoutPaidEvent(payoutRecord);
                            break;
                        default:

                    }
                });
    }

    private void emitPayoutPaidEvent(AdyenBalancePayoutReportRecord payoutRecord) {
        var payoutPaidEvent = PayoutPaid.from(payoutRecord.transferId(), payoutRecord.payoutDate(), payoutRecord.status());
        payoutEmitterService.emitPayoutEvent(payoutPaidEvent, payoutRecord.balanceAccount());
    }

    private void emitPaymentEvent(AdyenBalancePayoutReportRecord payoutRecord, AdyenBalancePayoutReportRecord bankTransferRecord) {
        var paymentEvent = new PaymentIncludedInPayout(payoutRecord.pspPaymentMerchantReference(),
                bankTransferRecord.transferId(),
                bankTransferRecord.payoutDate());
        emitEvent(paymentEvent, payoutRecord.pspPaymentMerchantReference());
        //LOG
    }

    private void emitRefundEvent(AdyenBalancePayoutReportRecord payoutRecord, AdyenBalancePayoutReportRecord bankTransferRecord) {
        var refundEvent = new RefundIncludedInPayout(payoutRecord.pspModificationMerchantReference(),
                bankTransferRecord.transferId(),
                bankTransferRecord.payoutDate());
        emitEvent(refundEvent, payoutRecord.pspPaymentMerchantReference());

        //LOG
    }

    private void emitEvent(Event event, String transactionExternalId) {
        if (TRUE.equals(connectorConfiguration.getEmitPayoutEvents())) {
            try {
                eventService.emitEvent(event, false);
            } catch (QueueException e) {
                throw new RuntimeException(format("Error sending %s event for transaction [%s] included in payout to event queue: %s",
                        event.getEventType(), transactionExternalId, e.getMessage()), e);
            }
        }
    }
}
