package uk.gov.pay.connector.queue.payout;

import com.adyen.model.reportwebhooks.ReportNotificationData;
import com.adyen.model.reportwebhooks.ReportNotificationRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenPayoutReconciliationPayload extends PayoutReconciliationPayload {

    private final String reportType;
    private final String balancePlatform;
    private final String creationDate;
    private final String downloadUrl;
    private final String fileName;
    private final String environment;

    public AdyenPayoutReconciliationPayload(@JsonProperty("report_type") String reportType,
                                            @JsonProperty("balance_platform") String balancePlatform,
                                            @JsonProperty("creation_date") String creationDate,
                                            @JsonProperty("download_url") String downloadUrl,
                                            @JsonProperty("file_name") String fileName,
                                            @JsonProperty("environment") String environment) {
        super(ADYEN.getName());
        this.reportType = reportType;
        this.balancePlatform = balancePlatform;
        this.creationDate = creationDate;
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.environment = environment;
    }

    public static AdyenPayoutReconciliationPayload from(ReportNotificationRequest reportNotificationRequest) {
        ReportNotificationData data = reportNotificationRequest.getData();
        return new AdyenPayoutReconciliationPayload(data.getReportType(),
                data.getBalancePlatform(),
                data.getCreationDate().toString(),
                data.getDownloadUrl(),
                data.getFileName(),
                reportNotificationRequest.getEnvironment());
    }

    public String getReportType() {
        return reportType;
    }

    public String getBalancePlatform() {
        return balancePlatform;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getEnvironment() {
        return environment;
    }
}
