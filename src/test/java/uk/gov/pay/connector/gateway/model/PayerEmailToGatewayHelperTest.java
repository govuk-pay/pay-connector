package uk.gov.pay.connector.gateway.model;

import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequestFixture;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture;
import uk.gov.pay.connector.model.domain.googlepay.GooglePayAuthRequestFixture;
import uk.gov.pay.connector.model.domain.googlepay.GooglePayPaymentInfoFixture;
import uk.gov.pay.connector.wallets.googlepay.GooglePayAuthorisationGatewayRequest;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequestFixture.aCardAuthorisationGatewayRequest;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;
import static uk.gov.pay.connector.model.domain.googlepay.GooglePayAuthRequestFixture.aGooglePayAuthRequest;
import static uk.gov.pay.connector.model.domain.googlepay.GooglePayPaymentInfoFixture.aGooglePayPaymentInfo;

class PayerEmailToGatewayHelperTest {

    private static final String EMAIL_ADDRESS = "test@test.test";

    private final GatewayAccountEntityFixture gatewayAccountEntityFixture = aGatewayAccountEntity();
    private final ChargeEntityFixture chargeEntityFixture = aValidChargeEntity();
    private final CardAuthorisationGatewayRequestFixture cardAuthorisationGatewayRequestFixture =
            aCardAuthorisationGatewayRequest();
    private final GooglePayAuthRequestFixture googlePayAuthRequestFixture = aGooglePayAuthRequest();
    private final GooglePayPaymentInfoFixture googlePayPaymentInfoFixture = aGooglePayPaymentInfo();

    private final PayerEmailToGatewayHelper payerEmailToGatewayHelper = new PayerEmailToGatewayHelper();

    @Test
    void returnsEmailForCardPaymentWhenSendPayerEmailToGatewayTrueAndEmailAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build())
                .withEmail(EMAIL_ADDRESS)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(EMAIL_ADDRESS));
    }

    @Test
    void returnsEmptyForCardPaymentWhenSendPayerEmailToGatewayTrueAndEmailNotAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build())
                .withEmail(null)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyForCardPaymentWhenSendPayerEmailToGatewayFalseAndEmailAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build())
                .withEmail(EMAIL_ADDRESS)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyForCardPaymentWhenSendPayerEmailToGatewayFalseAndEmailNotAvailable() {
        CardAuthorisationGatewayRequest request = cardAuthorisationGatewayRequestFixture
                .withGatewayAccount(gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build())
                .withEmail(null)
                .build();

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmailForGooglePayPaymentWhenSendPayerEmailToGatewayTrueAndEmailAvailable() {
        GooglePayAuthorisationGatewayRequest request = GooglePayAuthorisationGatewayRequest.valueOf(
                    chargeEntityFixture.withGatewayAccountEntity(
                                gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build()
                            ).build(),
                    googlePayAuthRequestFixture
                            .withGooglePaymentInfo(googlePayPaymentInfoFixture.withEmail(EMAIL_ADDRESS).build())
                            .build());

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(EMAIL_ADDRESS));
    }

    @Test
    void returnsEmptyForGooglePayPaymentWhenSendPayerEmailToGatewayTrueAndEmailNotAvailable() {
        GooglePayAuthorisationGatewayRequest request = GooglePayAuthorisationGatewayRequest.valueOf(
                chargeEntityFixture.withGatewayAccountEntity(
                            gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(true).build()
                        ).build(),
                googlePayAuthRequestFixture
                        .withGooglePaymentInfo(googlePayPaymentInfoFixture.withEmail(null).build())
                        .build());

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyForGooglePayPaymentWhenSendPayerEmailToGatewayFalseAndEmailAvailable() {
        GooglePayAuthorisationGatewayRequest request = GooglePayAuthorisationGatewayRequest.valueOf(
                chargeEntityFixture.withGatewayAccountEntity(
                            gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build()
                        ).build(),
                googlePayAuthRequestFixture
                        .withGooglePaymentInfo(googlePayPaymentInfoFixture.withEmail(EMAIL_ADDRESS).build())
                        .build());

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void returnsEmptyForGooglePayPaymentWhenSendPayerEmailToGatewayFalseAndEmailNotAvailable() {
        GooglePayAuthorisationGatewayRequest request = GooglePayAuthorisationGatewayRequest.valueOf(
                chargeEntityFixture.withGatewayAccountEntity(
                            gatewayAccountEntityFixture.withSendPayerEmailAddressToGateway(false).build()
                        ).build(),
                googlePayAuthRequestFixture
                        .withGooglePaymentInfo(googlePayPaymentInfoFixture.withEmail(null).build())
                        .build());

        Optional<String> result = payerEmailToGatewayHelper.getEmail(request);

        assertThat(result.isEmpty(), is(true));
    }

}
