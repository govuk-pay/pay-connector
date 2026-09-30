package uk.gov.pay.connector.gateway.adyen.request;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gateway.model.OrderRequestType.DOWNLOAD_BALANCE_PLATFORM_REPORT;

class AdyenDownloadBalancePlatformReportRequestTest {

    @Test
    void shouldReturnExpectedProperties() {
        URI url = URI.create("https://example.com/storedPaymentMethods/abc123");
        Map<String, String> headers = Map.of("X-API-Key", "a-test-key");
        String gatewayAccountType = "test";

        var request = new AdyenDownloadBalancePlatformReportRequest(url, headers, gatewayAccountType);

        assertThat(request.getUrl(), is(url));
        assertThat(request.getHeaders(), is(headers));
        assertThat(request.getQueryParams().size(), is(0));
        assertThat(request.getGatewayAccountType(), is(gatewayAccountType));
        assertThat(request.getOrderRequestType(), is(DOWNLOAD_BALANCE_PLATFORM_REPORT));
        assertThat(request.getPaymentProvider(), is(ADYEN));
    }
}
