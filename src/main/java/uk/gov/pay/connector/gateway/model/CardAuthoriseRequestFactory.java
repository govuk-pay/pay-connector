package uk.gov.pay.connector.gateway.model;

import jakarta.inject.Inject;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.records.CardAuthoriseRequest;
import uk.gov.pay.connector.gateway.worldpay.WorldpayCardAuthoriseRequestFactory;

import java.util.Optional;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.WORLDPAY;

public class CardAuthoriseRequestFactory {

    private final WorldpayCardAuthoriseRequestFactory worldpayCardAuthoriseRequestFactory;

    @Inject
    public CardAuthoriseRequestFactory(WorldpayCardAuthoriseRequestFactory worldpayCardAuthoriseRequestFactory) {
        this.worldpayCardAuthoriseRequestFactory = worldpayCardAuthoriseRequestFactory;
    }

    public Optional<? extends CardAuthoriseRequest> create(CardAuthorisationGatewayRequest request) {
        if (request.getPaymentGatewayName() == WORLDPAY) {
            return worldpayCardAuthoriseRequestFactory.create(request);
        }

        return Optional.empty();
    }

}
