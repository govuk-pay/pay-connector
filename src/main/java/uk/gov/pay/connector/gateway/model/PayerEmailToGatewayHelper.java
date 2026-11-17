package uk.gov.pay.connector.gateway.model;

import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;

import java.util.Optional;

public class PayerEmailToGatewayHelper {

    public Optional<String> getEmail(CardAuthorisationGatewayRequest request) {
        if (request.getGatewayAccount().isSendPayerEmailToGateway()) {
            return Optional.ofNullable(request.getEmail());
        }
        return Optional.empty();
    }

}
