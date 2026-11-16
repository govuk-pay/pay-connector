package uk.gov.pay.connector.gateway.model;

import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequestFixture;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequestFixture.aCardAuthorisationGatewayRequest;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;

class PayerEmailToGatewayHelperTest {

    private static final String EMAIL_ADDRESS = "test@test.test";

    private final GatewayAccountEntityFixture gatewayAccountEntityFixture = aGatewayAccountEntity();
    private final CardAuthorisationGatewayRequestFixture cardAuthorisationGatewayRequestFixture =
            aCardAuthorisationGatewayRequest();

    private final PayerEmailToGatewayHelper payerEmailToGatewayHelper = new PayerEmailToGatewayHelper();

    @Test
    void returnsEmailWhenSendPayerEmailToGatewayTrueAndEmailAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build())
                .withEmail(EMAIL_ADDRESS)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(EMAIL_ADDRESS));
    }

    @Test
    void returnsEmptyWhenSendPayerEmailToGatewayTrueAndEmailNotAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build())
                .withEmail(null)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyWhenSendPayerEmailToGatewayFalseAndEmailAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build())
                .withEmail(EMAIL_ADDRESS)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyWhenSendPayerEmailToGatewayFalseAndEmailNotAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build())
                .withEmail(null)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

}
