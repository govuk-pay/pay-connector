package uk.gov.pay.connector.gateway.model;

import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.wallets.googlepay.GooglePayAuthorisationGatewayRequest;

import java.util.Optional;

public class PayerEmailToGatewayHelper {

    public Optional<String> getEmail(CardAuthorisationGatewayRequest request) {
        return getEmail(request.getGatewayAccount(), request.getEmail());
    }

    public Optional<String> getEmail(GooglePayAuthorisationGatewayRequest request) {
        return getEmail(request.getGatewayAccount(), request.getGooglePayAuthorisationRequest().getPaymentInfo().getEmail());
    }

    private Optional<String> getEmail(GatewayAccountEntity gatewayAccountEntity, String email) {
        if (gatewayAccountEntity.isSendPayerEmailToGateway()) {
            return Optional.ofNullable(email);
        }
        return Optional.empty();
    }

}
