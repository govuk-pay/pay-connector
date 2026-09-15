package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdyenPayoutReconciliationPayload extends PayoutReconciliationPayload {

    private String reportId;
    private String balancePlatform;
    private String downloadUrl;
    private String fileName;
    private String reportType;

    public AdyenPayoutReconciliationPayload() {
        super();
    }

    public AdyenPayoutReconciliationPayload(String reportId, String balancePlatform, java.time.Instant createdDate,
                                            String downloadUrl, String fileName, String reportType) {
        super(ADYEN.getName(), createdDate);
        this.reportId = reportId;
        this.balancePlatform = balancePlatform;
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.reportType = reportType;
    }

    @JsonProperty("id")
    public String getReportId() {
        return reportId;
    }

    @JsonProperty("id")
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    @JsonProperty("balancePlatform")
    public String getBalancePlatform() {
        return balancePlatform;
    }

    @JsonProperty("balancePlatform")
    public void setBalancePlatform(String balancePlatform) {
        this.balancePlatform = balancePlatform;
    }

    @JsonProperty("downloadUrl")
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @JsonProperty("downloadUrl")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty("fileName")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @JsonProperty("reportType")
    public String getReportType() {
        return reportType;
    }

    @JsonProperty("reportType")
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    @Override
    public String toString() {
        return "AdyenPayoutReconciliationPayload{" +
                "reportId='" + reportId + '\'' +
                ", balancePlatform='" + balancePlatform + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                ", reportType='" + reportType + '\'' +
                '}';
    }
}
