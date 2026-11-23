package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccount.dao.AdyenAccountCapabilitiesDao;
import uk.gov.pay.connector.gatewayaccount.model.AdyenAccountCapabilitiesEntity;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccountcredentials.dao.GatewayAccountCredentialsDao;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenAccountHolderNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenAccountHolderNotificationHandler.class);

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

        if (accountHolder == null || accountHolder.getId() == null || accountHolder.getId().isBlank()) {
            LOGGER.atError()
                    .setMessage("Adyen account holder notification is missing account holder id")
                    .log();
            throw new AdyenNotificationException("Adyen account holder notification is missing account holder id");
        }

        var gatewayAccount = getGatewayAccount(accountHolder.getId());

        var maybeAdyenAccountCapabilities = adyenAccountCapabilitiesDao.findByGatewayAccountId(gatewayAccount.getId());

        var adyenAccountCapabilities = maybeAdyenAccountCapabilities.orElseGet(AdyenAccountCapabilitiesEntity::new);

        LOGGER.atInfo()
                .setMessage("Received a balancePlatform.accountHolder.updated event for Adyen legal entity {}")
                .addArgument(accountHolderNotificationData.getAccountHolder().getLegalEntityId())
                .addKeyValue("account_holder_id", accountHolder.getId())
                .log();
    }

    private GatewayAccountEntity getGatewayAccount(String accountHolderId) {
        var gatewayAccountCredentials = gatewayAccountCredentialsDao
                .findByPaymentProviderAndCredentialsKeyValue(
                        ADYEN.getName(),
                        AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                        accountHolderId
                );

        if (gatewayAccountCredentials.isEmpty()) {
            LOGGER.atError()
                    .setMessage("Adyen account holder notification capabilities are missing or empty")
                    .addKeyValue("accountHolderId", accountHolderId)
                    .log();
            throw new AdyenNotificationException("No Adyen gateway account credentials found for account holder webhook");
        }

        return gatewayAccountCredentials.get().getGatewayAccountEntity();
    }
}
