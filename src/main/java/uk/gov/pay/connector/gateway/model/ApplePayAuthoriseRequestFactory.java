package uk.gov.pay.connector.gateway.model;

import jakarta.inject.Inject;
import uk.gov.pay.connector.gateway.adyen.AdyenApplePayAuthorisePayloadFactory;
import uk.gov.pay.connector.gateway.model.request.records.ApplePayAuthoriseRequest;
import uk.gov.pay.connector.wallets.applepay.ApplePayAuthorisationGatewayRequest;

import java.util.Optional;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class ApplePayAuthoriseRequestFactory {

    private final AdyenApplePayAuthorisePayloadFactory adyenApplePayAuthorisePayloadFactory;

    @Inject
    public ApplePayAuthoriseRequestFactory(AdyenApplePayAuthorisePayloadFactory adyenApplePayAuthorisePayloadFactory) {
        this.adyenApplePayAuthorisePayloadFactory = adyenApplePayAuthorisePayloadFactory;
    }
    
    public Optional<? extends ApplePayAuthoriseRequest> create(ApplePayAuthorisationGatewayRequest request) {
        if (request.getPaymentGatewayName() == ADYEN) {
            return Optional.of(adyenApplePayAuthorisePayloadFactory.create(request));
        }

        return Optional.empty();
    }

}
