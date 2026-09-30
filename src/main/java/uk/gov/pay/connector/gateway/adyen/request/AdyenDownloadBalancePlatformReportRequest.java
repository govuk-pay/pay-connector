package uk.gov.pay.connector.gateway.adyen.request;

import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.model.OrderRequestType;
import uk.gov.pay.connector.gateway.model.request.GatewayClientGetRequest;

import java.net.URI;
import java.util.Map;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gateway.model.OrderRequestType.DOWNLOAD_BALANCE_PLATFORM_REPORT;

public record AdyenDownloadBalancePlatformReportRequest(URI url,
                                                        Map<String, String> headers,
                                                        String environment) implements GatewayClientGetRequest {
    @Override
    public URI getUrl() {
        return url;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getGatewayAccountType() {
        return environment;
    }

    @Override
    public PaymentGatewayName getPaymentProvider() {
        return ADYEN;
    }

    @Override
    public OrderRequestType getOrderRequestType() {
        return DOWNLOAD_BALANCE_PLATFORM_REPORT;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return Map.of();
    }
}
