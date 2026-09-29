package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.reportwebhooks.ReportNotificationData;
import com.adyen.model.reportwebhooks.ReportNotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileQueue;
import uk.gov.service.payments.commons.queue.exception.QueueException;

public class AdyenBalanceReportNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenBalanceReportNotificationHandler.class);

    private static final String REPORT_TYPE = "report_type";
    private static final String FILE_NAME = "file_name";
    private static final String BALANCE_PLATFORM_ID = "balance_platform_id";
    private static final String DOWNLOAD_URL = "download_url";
    private static final String ENVIRONMENT = "environment";

    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    private final PayoutReconcileQueue payoutReconcileQueue;

    @Inject
    public AdyenBalanceReportNotificationHandler(AdyenWebhookDeserialiser adyenWebhookDeserialiser,
                                                 PayoutReconcileQueue payoutReconcileQueue) {
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.payoutReconcileQueue = payoutReconcileQueue;
    }

    public void process(String notificationPayload) {
        ReportNotificationRequest reportNotificationRequest =
                adyenWebhookDeserialiser.deserialiseBalanceReportPayload(notificationPayload);
        ReportNotificationData reportNotificationData = reportNotificationRequest.getData();

        if (reportNotificationData == null) {
            LOGGER.error("Adyen balance transaction notification data is empty");
            throw new AdyenNotificationException("Adyen balance transaction notification data is empty");
        }

        MDC.put(REPORT_TYPE, reportNotificationData.getReportType());
        MDC.put(FILE_NAME, reportNotificationData.getFileName());
        MDC.put(BALANCE_PLATFORM_ID, reportNotificationData.getBalancePlatform());
        MDC.put(DOWNLOAD_URL, reportNotificationData.getDownloadUrl());
        MDC.put(ENVIRONMENT, reportNotificationRequest.getEnvironment());

        try {
            if ("balanceplatform_payout_report".equals(reportNotificationData.getReportType())) {
                AdyenPayoutReconciliationPayload reconciliationPayload = AdyenPayoutReconciliationPayload.from(reportNotificationRequest);
                payoutReconcileQueue.sendPayout(reconciliationPayload);
            } else {
                LOGGER.atInfo()
                        .setMessage("Ignoring Adyen balance platform report notification")
                        .log();
            }
        } catch (QueueException | JsonProcessingException e) {
            LOGGER.atError()
                    .setMessage("Error sending Adyen payout report to payout reconcile queue")
                    .setCause(e)
                    .log();
            throw new WebApplicationException("Error sending Adyen payout report to payout reconcile queue");
        } finally {
            MDC.remove(REPORT_TYPE);
            MDC.remove(FILE_NAME);
            MDC.remove(DOWNLOAD_URL);
            MDC.remove(ENVIRONMENT);
            MDC.remove(BALANCE_PLATFORM_ID);
        }
    }
}
