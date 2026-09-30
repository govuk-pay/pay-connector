package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.gateway.adyen.AdyenPaymentProvider;
import uk.gov.pay.connector.gateway.adyen.report.AdyenPayoutReportParser;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileHandler;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;

public class AdyenPayoutReconciliationHandler implements PayoutReconcileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenPayoutReconciliationHandler.class);

    private final AdyenPayoutReportParser adyenPayoutReportParser;
    private final AdyenPaymentProvider adyenPaymentProvider;

    @Inject
    public AdyenPayoutReconciliationHandler(PaymentProviders paymentProviders,
                                            AdyenPayoutReportParser adyenPayoutReportParser) {
        this.adyenPayoutReportParser = adyenPayoutReportParser;
        adyenPaymentProvider = (AdyenPaymentProvider) paymentProviders.byName(PaymentGatewayName.ADYEN);
    }

    public boolean reconcile(PayoutReconcileMessage payoutReconcileMessage) {

        AdyenPayoutReconciliationPayload payout = (AdyenPayoutReconciliationPayload) payoutReconcileMessage.getPayout();
        try {
            String csv = adyenPaymentProvider.downloadReport(payout);
            adyenPayoutReportParser.parse(csv);
        } catch (Exception e) {
            LOGGER.error("Failed to reconcile Adyen Payout report", e);
            return false;
        }
        return true;
    }
}
