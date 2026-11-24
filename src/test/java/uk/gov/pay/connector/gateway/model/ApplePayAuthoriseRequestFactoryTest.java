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
import uk.gov.pay.connector.gateway.adyen.AdyenApplePayAuthorisePayloadFactory;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenCredentialsHelper;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenMerchantAccountHelper;
import uk.gov.pay.connector.gateway.model.request.records.AdyenApplePayAuthorisePayload;
import uk.gov.pay.connector.gateway.model.request.records.AdyenApplePayAuthorisePayloadFixture;
import uk.gov.pay.connector.gateway.model.request.records.ApplePayAuthoriseRequest;
import uk.gov.pay.connector.gateway.util.ChargeFrontendUrlHelper;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;
import uk.gov.pay.connector.model.domain.applepay.ApplePayAuthRequestFixture;
import uk.gov.pay.connector.wallets.applepay.ApplePayAuthorisationGatewayRequest;
import uk.gov.pay.connector.wallets.applepay.api.ApplePayAuthRequest;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApplePayAuthoriseRequestFactoryTest {

    @Mock
    private AdyenApplePayAuthorisePayloadFactory mockAdyenApplePayAuthorisePayloadFactory;
    @Mock
    private AdyenMerchantAccountHelper mockAdyenMerchantAccountHelper;
    @Mock
    private AdyenCredentialsHelper mockAdyenCredentialsHelper;
    @Mock
    private ChargeFrontendUrlHelper mockChargeFrontendUrlHelper;
    
    private ApplePayAuthoriseRequestFactory authoriseRequestFactory;

    private final AdyenCredentials adyenCredentials = new AdyenCredentials(
            "legalEntityId",
            "storeId",
            "accountHolderId",
            "balanceAccountId"
    );

    @BeforeEach
    void setUp() {
        authoriseRequestFactory = new ApplePayAuthoriseRequestFactory(mockAdyenApplePayAuthorisePayloadFactory);

    }

    @Test
    void shouldBuildAydenAuthoriseRequestIfAdyen() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withPaymentProvider(PaymentGatewayName.ADYEN.getName())
                .build();
        
        ApplePayAuthRequest applePayAuthRequest = ApplePayAuthRequestFixture.anApplePayAuthRequest().build();

        ApplePayAuthorisationGatewayRequest applePayAuthorisationGatewayRequest = ApplePayAuthorisationGatewayRequest.valueOf(chargeEntity, applePayAuthRequest);
        
        AdyenApplePayAuthorisePayload adyenApplePayAuthorisePayload = AdyenApplePayAuthorisePayloadFixture.anAdyenApplePayAuthorisePayloadFixture().build();

        given(mockAdyenApplePayAuthorisePayloadFactory.create(applePayAuthorisationGatewayRequest)).willReturn(adyenApplePayAuthorisePayload);

        Optional<? extends ApplePayAuthoriseRequest> applePayAuthoriseRequest = authoriseRequestFactory.create(applePayAuthorisationGatewayRequest);
        
        assertThat(applePayAuthoriseRequest.isPresent(), is(true));
        assertThat(applePayAuthoriseRequest.get(), is(adyenApplePayAuthorisePayload));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentGatewayName.class,  mode = EXCLUDE, names = { "ADYEN" })
    void shouldBuildNothingIfNotAdyen(PaymentGatewayName paymentGatewayName) {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withPaymentProvider(paymentGatewayName.getName())
                .build();
        
        ApplePayAuthRequest applePayAuthRequest = ApplePayAuthRequestFixture.anApplePayAuthRequest().build();

        ApplePayAuthorisationGatewayRequest applePayAuthorisationGatewayRequest = ApplePayAuthorisationGatewayRequest.valueOf(chargeEntity, applePayAuthRequest);

        Optional<? extends ApplePayAuthoriseRequest> applePayAuthoriseRequest = authoriseRequestFactory.create(applePayAuthorisationGatewayRequest);
        
        assertThat(applePayAuthoriseRequest.isEmpty(), is(true));
    }

}
