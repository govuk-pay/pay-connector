package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccount.dao.AdyenAccountCapabilitiesDao;
import uk.gov.pay.connector.gatewayaccount.model.AdyenAccountCapabilitiesEntity;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;
import uk.gov.pay.connector.gatewayaccountcredentials.dao.GatewayAccountCredentialsDao;
import uk.gov.pay.connector.queue.tasks.model.CapabilityUpdate;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenAccountHolderNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenAccountHolderNotificationHandler.class);
    
    private static final String VALID = "valid";
    private static final String INVALID = "invalid";
    private static final String REJECTED = "rejected";


    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    private final GatewayAccountCredentialsDao gatewayAccountCredentialsDao;
    private final AdyenAccountCapabilitiesDao adyenAccountCapabilitiesDao;
    
    @Inject
    public AdyenAccountHolderNotificationHandler(AdyenWebhookDeserialiser adyenWebhookDeserialiser,
                                                 GatewayAccountCredentialsDao gatewayAccountCredentialsDao,
                                                 AdyenAccountCapabilitiesDao adyenAccountCapabilitiesDao) {
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.gatewayAccountCredentialsDao = gatewayAccountCredentialsDao;
        this.adyenAccountCapabilitiesDao = adyenAccountCapabilitiesDao;
    }

    @Transactional
    public void process(String payload) {
        var accountHolderNotificationRequest = adyenWebhookDeserialiser.deserialiseAccountHolderPayload(payload);
        var accountHolderNotificationData = accountHolderNotificationRequest.getData();
        
        if (accountHolderNotificationData == null) {
            LOGGER.atError()
                    .setMessage("Adyen account holder notification data is empty")
                    .log();
            throw new AdyenNotificationException("Adyen account holder notification data is empty");
        }
        
        var accountHolder = accountHolderNotificationData.getAccountHolder();
        
        if (accountHolder == null || accountHolder.getId() == null || accountHolder.getId().isBlank()){
            LOGGER.atError()
                    .setMessage("Adyen account holder notification is missing account holder id")
                    .log();
            throw new AdyenNotificationException("Adyen account holder notification is missing account holder id");
        }

        LOGGER.atInfo()
                .setMessage("Received a balancePlatform.accountHolder.updated event for Adyen legal entity {}")
                .addArgument(accountHolder.getLegalEntityId())
                .addKeyValue("account_holder_id", accountHolder.getId())
                .log();
        
        var gatewayAccountCredentials = gatewayAccountCredentialsDao
                .findByPaymentProviderAndCredentialsKeyValue(
                        ADYEN.getName(),
                        AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                        accountHolder.getId()
                );
        
        if (gatewayAccountCredentials.isEmpty()) {
            LOGGER.atError()
                    .setMessage("Adyen account holder notification capabilties are missing or empty")
                    .addKeyValue("gateway_account_id", accountHolder.getId())
                    .log();
            throw new AdyenNotificationException("No Adyen gateway account credentials found for account holder webhook");
        }
        
        var gatewayAccount = gatewayAccountCredentials.get().getGatewayAccountEntity();
        
    }
    
    private CapabilityUpdate parseCapabilitiesUpdate (String capabilityName, JsonNode capabilityValue, Long gatewayAccountId) {
        return null;
    }
    
    private void SetCapabilities(AdyenAccountCapabilitiesEntity adyenAccountCapabilitiesEntity, CapabilityUpdate capabilityUpdate) {
        switch (capabilityUpdate.capabilityField()) {
            case RECEIVE_PAYMENT -> adyenAccountCapabilitiesEntity.setReceivePayments(capabilityUpdate.value());
            case SEND_TO_BALANCE_ACCOUNT -> adyenAccountCapabilitiesEntity.setSendToBalanceAccount(capabilityUpdate.value());
            case SEND_TO_TRANSFER_INSTRUMENT -> adyenAccountCapabilitiesEntity.setSendToTransferInstrument(capabilityUpdate.value());
            case RECEIVE_FROM_BALANCE_ACCOUNT -> adyenAccountCapabilitiesEntity.setReceiveFromBalanceAccount(capabilityUpdate.value());
            case RECEIVE_FROM_PLATFORM_PAYMENT -> adyenAccountCapabilitiesEntity.setReceiveFromPlatformAccount(capabilityUpdate.value());
        }
    }
}
