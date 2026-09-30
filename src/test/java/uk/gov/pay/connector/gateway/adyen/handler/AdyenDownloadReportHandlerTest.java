package uk.gov.pay.connector.gateway.adyen.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.app.adyen.ApiKeys;
import uk.gov.pay.connector.gateway.GatewayClient;
import uk.gov.pay.connector.gateway.GatewayException;
import uk.gov.pay.connector.gateway.adyen.request.AdyenDownloadBalancePlatformReportRequest;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.app.adyen.ApiKeysFixture.someApiKeys;

@ExtendWith(MockitoExtension.class)
class AdyenDownloadReportHandlerTest {

    private static final String REPORT_TYPE = "balanceplatform_payout_report";
    private static final String DOWNLOAD_URL = "https://example.com/report.csv";
    private static final String FILE_NAME = "report.csv";
    private static final String ENVIRONMENT = "test";

    @Mock
    private GatewayClient gatewayClient;

    @Mock
    private ConnectorConfiguration connectorConfiguration;

    @Mock
    private AdyenGatewayConfig adyenGatewayConfig;

    private AdyenPayoutReconciliationPayload payload;

    @Mock
    private GatewayClient.Response mockResponse;

    @Captor
    ArgumentCaptor<AdyenDownloadBalancePlatformReportRequest> requestArgumentCaptor;

    private AdyenDownloadReportHandler handler;

    @BeforeEach
    void setUp() {
        ApiKeys apiKeys = someApiKeys()
                .withBalancePlatformReportApiKeys(
                        new ApiKeys.BalancePlatformReportApiKeys("test-api-key", "live-api-key"))
                .build();

        when(connectorConfiguration.getAdyenGatewayConfig()).thenReturn(adyenGatewayConfig);
        when(adyenGatewayConfig.getApiKeys()).thenReturn(apiKeys);

        handler = new AdyenDownloadReportHandler(gatewayClient, connectorConfiguration);


        payload = new AdyenPayoutReconciliationPayload(
                REPORT_TYPE, null, null,
                DOWNLOAD_URL, FILE_NAME, ENVIRONMENT);
    }

    @Test
    void shouldDownloadCsvReport() throws Exception {
        var csv = """
                BalanceAccount,Transfer Id,Balance (PC)
                BA00000000000000000000001,3JY1Y65VXYY7GD3F,1.00
                BA00000000000000000000001,3CY1XOPVXWKYA3O9,-1.00
                """;

        when(gatewayClient.getRequestFor(any(AdyenDownloadBalancePlatformReportRequest.class)))
                .thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(csv);

        var result = handler.downloadReport(payload);

        assertThat(result, is(csv));

        verify(gatewayClient).getRequestFor(requestArgumentCaptor.capture());
        verify(mockResponse).getEntity();

        AdyenDownloadBalancePlatformReportRequest requestData = requestArgumentCaptor.getValue();
        assertThat(requestData.getGatewayAccountType(), is("test"));
        assertThat(requestData.getUrl().toString(), is("https://example.com/report.csv"));
        assertThat(requestData.getHeaders(), is(Map.of("X-API-Key", "test-api-key")));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenDownloadFails() throws Exception {
        var gatewayException = new GatewayException.GenericGatewayException("Download failed");

        when(gatewayClient.getRequestFor(any(AdyenDownloadBalancePlatformReportRequest.class)))
                .thenThrow(gatewayException);

        var exception = assertThrows(RuntimeException.class,
                () -> handler.downloadReport(payload));

        assertThat(exception.getMessage(), is("Error downloading Adyen report"));
        assertThat(exception.getCause(), is(gatewayException));

        verify(gatewayClient).getRequestFor(any(AdyenDownloadBalancePlatformReportRequest.class));
        verifyNoInteractions(mockResponse);
    }
}
