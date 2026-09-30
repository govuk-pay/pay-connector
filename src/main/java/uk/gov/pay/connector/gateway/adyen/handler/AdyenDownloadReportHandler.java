package uk.gov.pay.connector.gateway.adyen.handler;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.gateway.GatewayClient;
import uk.gov.pay.connector.gateway.GatewayException;
import uk.gov.pay.connector.gateway.adyen.request.AdyenDownloadBalancePlatformReportRequest;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;

import java.net.URI;

import static uk.gov.pay.connector.gateway.adyen.utils.AdyenRequestUtil.getBalancePlatformReportApiKeyHeader;

public class AdyenDownloadReportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenDownloadReportHandler.class);

    private final GatewayClient gatewayClient;
    private final AdyenGatewayConfig adyenGatewayConfig;

    @Inject
    public AdyenDownloadReportHandler(GatewayClient gatewayClient,
                                      ConnectorConfiguration connectorConfig) {
        this.gatewayClient = gatewayClient;
        this.adyenGatewayConfig = connectorConfig.getAdyenGatewayConfig();
    }

    public String downloadReport(AdyenPayoutReconciliationPayload payoutReconciliationPayload) {
        var downloadBalanceReportRequest = new AdyenDownloadBalancePlatformReportRequest(
                URI.create(payoutReconciliationPayload.getDownloadUrl()),
                getBalancePlatformReportApiKeyHeader(adyenGatewayConfig, payoutReconciliationPayload.isLive()),
                payoutReconciliationPayload.getEnvironment());

        try {
            return gatewayClient.getRequestFor(downloadBalanceReportRequest).getEntity();
        } catch (GatewayException e) {
            LOGGER.atError()
                    .setMessage("Error downloading Adyen report")
                    .addKeyValue("report_type", payoutReconciliationPayload.getReportType())
                    .addKeyValue("file_name", payoutReconciliationPayload.getFileName())
                    .log();
            throw new RuntimeException("Error downloading Adyen report", e);
        }
    }
}
