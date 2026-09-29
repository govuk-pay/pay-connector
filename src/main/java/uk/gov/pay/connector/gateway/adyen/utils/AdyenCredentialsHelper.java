package uk.gov.pay.connector.gateway.adyen.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.model.request.GatewayRequest;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;

public class AdyenCredentialsHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenCredentialsHelper.class);
    
    public String getStore(GatewayRequest gatewayRequest) {
        AdyenCredentials adyenCredentials = getAdyenCredentials(gatewayRequest);
        if (adyenCredentials.storeId() != null) {
             return adyenCredentials.storeId();
        }
        LOGGER.error("Adyen storeId cannot be null for {}: {}", gatewayRequest.getGatewayAccount().getId(), adyenCredentials);
        throw new IllegalArgumentException("Adyen storeId cannot be null");
    }
    
    private AdyenCredentials getAdyenCredentials(GatewayRequest gatewayRequest){
        var gatewayCredentials = gatewayRequest.getGatewayCredentials();
        
        if (gatewayCredentials instanceof AdyenCredentials adyenCredentials) {
            return adyenCredentials;
        }

        throw new IllegalArgumentException("Expected provided GatewayCredentials to be of type AdyenCredentials");
    }

}
