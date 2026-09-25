package uk.gov.pay.connector.queue.payout;


public class AdyenPayoutReconciliationPayloadFixture {

    private String reportType = "balanceplatform_payout_report";
    private String balancePlatform = "pay_test";
    private String creationDate = "2026-09-24T00:04:12+02:00";
    private String downloadUrl = "https://some-url";
    private String fileName = "balanceplatform_payout_report_2026_09_24.csv";
    private String environment = "test";

    public static AdyenPayoutReconciliationPayloadFixture anAdyenPayoutReconciliationPayloadFixture() {
        return new AdyenPayoutReconciliationPayloadFixture();
    }

    public AdyenPayoutReconciliationPayload build() {
        return new AdyenPayoutReconciliationPayload(
                reportType,
                balancePlatform,
                creationDate,
                downloadUrl,
                fileName,
                environment
        );
    }
}
