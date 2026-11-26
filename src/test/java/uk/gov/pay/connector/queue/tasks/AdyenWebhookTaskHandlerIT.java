package uk.gov.pay.connector.queue.tasks;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.connector.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.connector.queue.capture.CaptureQueue;
import uk.gov.pay.connector.queue.tasks.handlers.adyen.AdyenWebhookTaskHandler;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountType.LIVE;
import static uk.gov.pay.connector.util.AddGatewayAccountCredentialsParams.AddGatewayAccountCredentialsParamsBuilder.anAddGatewayAccountCredentialsParams;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_CONFIGURATION_NOTIFICATION;

public class AdyenWebhookTaskHandlerIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();
    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(CaptureQueue.class);

    @Test
    void shouldProcessAdyenConfigurationWebhook() {
        AdyenWebhookTaskHandler taskHandler = app.getInstanceFromGuiceContainer(AdyenWebhookTaskHandler.class);

        var serviceId = "service-123";
        var adyenGatewayAccountId = 500L;
        var accountHolderId = "AH32DBQ22322CV5QCBNZ99XZ2";
        var adyenCredentialsParams = anAddGatewayAccountCredentialsParams()
                .withGatewayAccountId(adyenGatewayAccountId)
                .withPaymentProvider(ADYEN.getName())
                .withCredentials(Map.of("legal_entity_id", "LEM0000000000000001",
                        "store_id", "ST00000000000000000000001",
                        "account_holder_id", accountHolderId,
                        "balance_account_id", "BA0000000000000000000001"))
                .build();

        app.getDatabaseFixtures()
                .aTestAccount()
                .withAccountId(adyenGatewayAccountId)
                .withServiceId(serviceId)
                .withPaymentProvider(ADYEN.getName())
                .withType(LIVE)
                .withGatewayAccountCredentials(Collections.singletonList(adyenCredentialsParams))
                .insert();

        String payload = TestTemplateResourceLoader.load(ADYEN_CONFIGURATION_NOTIFICATION)
                .replace("{{receivePayments_verificationStatus}}", "invalid")
                .replace("{{sendToBalanceAccount_verificationStatus}}", "valid")
                .replace("{{accountHolderId}}", accountHolderId)
                .replace("{{environment}}", "test");

        taskHandler.processAdyenConfigurationWebhookNotification(payload);

        var adyenAccountCapabilities = app.getDatabaseTestHelper().getAdyenAccountCapabilities(adyenGatewayAccountId).getFirst();

        assertThat(adyenAccountCapabilities, hasEntry("receive_from_platform_payments", true));
        assertThat(adyenAccountCapabilities, hasEntry("receive_from_balance_account", true));
        assertThat(adyenAccountCapabilities, hasEntry("send_to_balance_account", true));
        assertThat(adyenAccountCapabilities, hasEntry("send_to_transfer_instrument", false));
        assertThat(adyenAccountCapabilities, hasEntry("receive_payments", false));

        // Second submission to check handling of repeated webhooks
        String secondPayload = TestTemplateResourceLoader.load(ADYEN_CONFIGURATION_NOTIFICATION)
                .replace("{{receivePayments_verificationStatus}}", "valid")
                .replace("{{sendToBalanceAccount_verificationStatus}}", "invalid")
                .replace("{{accountHolderId}}", accountHolderId)
                .replace("{{environment}}", "test");
        
        taskHandler.processAdyenConfigurationWebhookNotification(secondPayload);

        adyenAccountCapabilities = app.getDatabaseTestHelper().getAdyenAccountCapabilities(adyenGatewayAccountId).getFirst();

        assertThat(adyenAccountCapabilities, hasEntry("receive_from_platform_payments", true));
        assertThat(adyenAccountCapabilities, hasEntry("receive_from_balance_account", true));
        assertThat(adyenAccountCapabilities, hasEntry("send_to_balance_account", false));
        assertThat(adyenAccountCapabilities, hasEntry("send_to_transfer_instrument", false));
        assertThat(adyenAccountCapabilities, hasEntry("receive_payments", true));
    }
}
