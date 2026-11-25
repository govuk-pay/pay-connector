package uk.gov.pay.connector.gateway.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.adyen.AdyenGooglePayAuthorisePayloadFactory;
import uk.gov.pay.connector.gateway.model.request.records.AdyenGooglePayAuthorisePayload;
import uk.gov.pay.connector.gateway.model.request.records.AdyenGooglePayAuthorisePayloadFixture;
import uk.gov.pay.connector.gateway.model.request.records.GooglePayAuthoriseRequest;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;
import uk.gov.pay.connector.model.domain.googlepay.GooglePayAuthRequestFixture;
import uk.gov.pay.connector.wallets.googlepay.GooglePayAuthorisationGatewayRequest;
import uk.gov.pay.connector.wallets.googlepay.api.GooglePayAuthRequest;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GooglePayAuthoriseRequestFactoryTest {
    @Mock
    private AdyenGooglePayAuthorisePayloadFactory mockAdyenGooglePayAuthoriseRequestFactory;

    private GooglePayAuthoriseRequestFactory authoriseRequestFactory;

    private final AdyenCredentials adyenCredentials = new AdyenCredentials(
            "legalEntityId",
            "storeId",
            "accountHolderId",
            "balanceAccountId"
    );

    @BeforeEach
    void setUp() {
        authoriseRequestFactory = new GooglePayAuthoriseRequestFactory(mockAdyenGooglePayAuthoriseRequestFactory);

    }

    @Test
    void shouldBuildAydenAuthoriseRequestIfAdyen() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withPaymentProvider(PaymentGatewayName.ADYEN.getName())
                .build();

        GooglePayAuthRequest googlePayAuthRequest = GooglePayAuthRequestFixture.aGooglePayAuthRequest().build();

        GooglePayAuthorisationGatewayRequest googlePayAuthorisationGatewayRequest = GooglePayAuthorisationGatewayRequest.valueOf(chargeEntity, googlePayAuthRequest);

        AdyenGooglePayAuthorisePayload adyenGooglePayAuthorisePayload = AdyenGooglePayAuthorisePayloadFixture.anAdyenGooglePayAuthorisePayloadFixture().build();

        given(mockAdyenGooglePayAuthoriseRequestFactory.create(googlePayAuthorisationGatewayRequest)).willReturn(adyenGooglePayAuthorisePayload);

        Optional<? extends GooglePayAuthoriseRequest> googlePayAuthoriseRequest = authoriseRequestFactory.create(googlePayAuthorisationGatewayRequest);

        assertThat(googlePayAuthoriseRequest.isPresent(), is(true));
        assertThat(googlePayAuthoriseRequest.get(), is(adyenGooglePayAuthorisePayload));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentGatewayName.class,  mode = EXCLUDE, names = { "ADYEN" })
    void shouldBuildNothingIfNotAdyen(PaymentGatewayName paymentGatewayName) {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withPaymentProvider(paymentGatewayName.getName())
                .build();

        GooglePayAuthRequest googlePayAuthRequest = GooglePayAuthRequestFixture.aGooglePayAuthRequest().build();

        GooglePayAuthorisationGatewayRequest googlePayAuthorisationGatewayRequest = GooglePayAuthorisationGatewayRequest.valueOf(chargeEntity, googlePayAuthRequest);

        Optional<? extends GooglePayAuthoriseRequest> googlePayAuthoriseRequest = authoriseRequestFactory.create(googlePayAuthorisationGatewayRequest);

        assertThat(googlePayAuthoriseRequest.isEmpty(), is(true));
    }
}
