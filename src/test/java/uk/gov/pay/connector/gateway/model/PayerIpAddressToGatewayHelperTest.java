package uk.gov.pay.connector.gateway.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;

@ExtendWith(MockitoExtension.class)
class PayerIpAddressToGatewayHelperTest {

    private static final String IP_ADDRESS = "203.0.113.1";

    @Mock
    private BrowserDataFor3ds mockBrowserDataFor3ds;

    private final GatewayAccountEntityFixture gatewayAccountEntityFixture = aGatewayAccountEntity();

    private final PayerIpAddressToGatewayHelper payerIpAddressToGatewayHelper = new PayerIpAddressToGatewayHelper();

    @Test
    void returnsEmailWhenSendPayerIpAddressToGatewayTrueAndIpAvailable() {
        var gatewayAccountEntity = gatewayAccountEntityFixture.withSendPayerIpAddressToGateway(true).build();

        given(mockBrowserDataFor3ds.getBrowserIpAddress()).willReturn(Optional.of(IP_ADDRESS));

        Optional<String> result = payerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, mockBrowserDataFor3ds);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(IP_ADDRESS));
    }

    @Test
    void returnsEmptyWhenSendPayerIpAddressToGatewayTrueAndIpNotAvailable() {
        var gatewayAccountEntity = gatewayAccountEntityFixture.withSendPayerIpAddressToGateway(true).build();

        given(mockBrowserDataFor3ds.getBrowserIpAddress()).willReturn(Optional.empty());

        Optional<String> result = payerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, mockBrowserDataFor3ds);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyWhenSendPayerIpAddressToGatewayFalse() {
        var gatewayAccountEntity = gatewayAccountEntityFixture.withSendPayerIpAddressToGateway(false).build();

        Optional<String> result = payerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, mockBrowserDataFor3ds);

        assertThat(result.isEmpty(), is(true));
    }

}
