package uk.gov.pay.connector.gateway.model;

import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;

import java.util.Optional;

public class PayerIpAddressToGatewayHelper {

    public Optional<String> getIpAddress(GatewayAccountEntity gatewayAccountEntity, BrowserDataFor3ds browserDataFor3ds) {
        if (gatewayAccountEntity.isSendPayerIpAddressToGateway()) {
            return browserDataFor3ds.getBrowserIpAddress();
        }
        return Optional.empty();
    }

}
