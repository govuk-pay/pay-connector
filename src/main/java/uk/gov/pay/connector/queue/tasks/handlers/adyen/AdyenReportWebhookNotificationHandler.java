package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileQueue;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class AdyenReportWebhookNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenReportWebhookNotificationHandler.class);
    private static final DateTimeFormatter ADYEN_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final PayoutReconcileQueue payoutReconcileQueue;

    @Inject
    public AdyenReportWebhookNotificationHandler(ObjectMapper objectMapper,
                                                  PayoutReconcileQueue payoutReconcileQueue) {
        this.objectMapper = objectMapper;
        this.payoutReconcileQueue = payoutReconcileQueue;
    }

    public void handle(String payload) {
        try {
            AdyenReportWebhookData webhookData = objectMapper.readValue(payload, AdyenReportWebhookData.class);
            
            LOGGER.info("Processing Adyen report webhook: reportId [{}], fileName [{}], reportType [{}]",
                    webhookData.getData().getId(), webhookData.getData().getFileName(), webhookData.getData().getReportType());

            if (!"balanceplatform_payout_report".equals(webhookData.getData().getReportType())) {
                LOGGER.info("Ignoring non-payout report type: {}", webhookData.getData().getReportType());
                return;
            }

            Instant creationDate = Instant.parse(webhookData.getData().getCreationDate());
            
            AdyenPayoutReconciliationPayload adyenPayout = new AdyenPayoutReconciliationPayload(
                    webhookData.getData().getId(),
                    webhookData.getData().getBalancePlatform(),
                    creationDate,
                    webhookData.getData().getDownloadUrl(),
                    webhookData.getData().getFileName(),
                    webhookData.getData().getReportType()
            );

            addToPayoutReconcileQueue(adyenPayout);

            LOGGER.info("Added Adyen payout [{}] to reconcile queue", webhookData.getData().getId());
        } catch (Exception e) {
            LOGGER.error("Error processing Adyen report webhook notification: {}", e.getMessage(), e);
        }
    }

    private void addToPayoutReconcileQueue(AdyenPayoutReconciliationPayload adyenPayout) {
        try {
            payoutReconcileQueue.sendPayout(adyenPayout);
        } catch (QueueException | com.fasterxml.jackson.core.JsonProcessingException e) {
            LOGGER.error("Error sending Adyen payout to payout reconcile queue: exception [{}]", e.getMessage());
            throw new RuntimeException("Failed to send Adyen payout to reconcile queue", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdyenReportWebhookData {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("environment")
        private String environment;
        
        @JsonProperty("data")
        private AdyenReportData data;
        
        @JsonProperty("timestamp")
        private String timestamp;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public AdyenReportData getData() {
            return data;
        }

        public void setData(AdyenReportData data) {
            this.data = data;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdyenReportData {
        @JsonProperty("balancePlatform")
        private String balancePlatform;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("creationDate")
        private String creationDate;
        
        @JsonProperty("fileName")
        private String fileName;
        
        @JsonProperty("reportType")
        private String reportType;
        
        @JsonProperty("downloadUrl")
        private String downloadUrl;

        public String getBalancePlatform() {
            return balancePlatform;
        }

        public void setBalancePlatform(String balancePlatform) {
            this.balancePlatform = balancePlatform;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(String creationDate) {
            this.creationDate = creationDate;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getReportType() {
            return reportType;
        }

        public void setReportType(String reportType) {
            this.reportType = reportType;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }
}
