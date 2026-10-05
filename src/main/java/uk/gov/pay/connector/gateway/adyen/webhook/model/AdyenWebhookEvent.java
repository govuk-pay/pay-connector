package uk.gov.pay.connector.gateway.adyen.webhook.model;

import java.util.Arrays;
import java.util.Optional;

import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.BALANCE_PLATFORM_REPORT;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.CONFIGURATION;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.PAYMENTS;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.TOKENS;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.TRANSFER;

public enum AdyenWebhookEvent {
    // payment events
    AUTHORISATION("AUTHORISATION", PAYMENTS, false),
    CANCELLATION("CANCELLATION", PAYMENTS, false),
    CAPTURE("CAPTURE", PAYMENTS, false),
    CAPTURE_FAILED("CAPTURE_FAILED", PAYMENTS, false),
    REFUND("REFUND", PAYMENTS, false),
    REFUND_FAILED("REFUND_FAILED", PAYMENTS, false),
    REFUNDED_REVERSED("REFUNDED_REVERSED", PAYMENTS, false),
    EXPIRE("EXPIRE", PAYMENTS, true),

    // tokens
    RECURRING_TOKEN_CREATED("recurring.token.created", TOKENS, false),
    RECURRING_TOKEN_DISABLED("recurring.token.disabled", TOKENS, false),

    // transfer
    TRANSFER_CREATED("balancePlatform.transfer.created", TRANSFER, false),
    TRANSFER_UPDATED("balancePlatform.transfer.updated", TRANSFER, false),

    // balance platform report
    BALANCE_PLATFORM_REPORT_CREATED("balancePlatform.report.created", BALANCE_PLATFORM_REPORT, false),
    
    // configuration
    ACCOUNT_HOLDER_CREATED("balancePlatform.accountHolder.created", CONFIGURATION, true),
    ACCOUNT_HOLDER_UPDATED("balancePlatform.accountHolder.updated", CONFIGURATION, false),
    BALANCE_ACCOUNT_CREATED("balancePlatform.balanceAccount.created", CONFIGURATION, true),
    BALANCE_ACCOUNT_UPDATED("balancePlatform.balanceAccount.updated", CONFIGURATION, true),
    BALANCE_ACCOUNT_SWEEP_CREATED("balancePlatform.balanceAccountSweep.created", CONFIGURATION, true),
    BALANCE_ACCOUNT_SWEEP_DELETED("balancePlatform.balanceAccountSweep.deleted", CONFIGURATION, true),
    BALANCE_ACCOUNT_SWEEP_UPDATED("balancePlatform.balanceAccountSweep.updated", CONFIGURATION, true),
    PAYMENT_INSTRUMENT_CREATED("balancePlatform.paymentInstrument.created", CONFIGURATION, true),
    PAYMENT_INSTRUMENT_EXPIRING("balancePlatform.paymentInstrument.expiring", CONFIGURATION, true),
    PAYMENT_INSTRUMENT_UPDATED("balancePlatform.paymentInstrument.updated", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_CREATED("balancePlatform.balanceAccountPayoutSchedule.created", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_UPDATED("balancePlatform.balanceAccountPayoutSchedule.updated", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_DELETED("balancePlatform.balanceAccountPayoutSchedule.deleted", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_AUTO_APPLICATION_FAILED("balancePlatform.balanceAccountPayoutScheduleAutoApplication.failed", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_EXECUTION_SUCCEEDED("balancePlatform.balanceAccountPayoutScheduleExecution.succeeded", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_EXECUTION_SKIPPED("balancePlatform.balanceAccountPayoutScheduleExecution.skipped", CONFIGURATION, true),
    BALANCE_ACCOUNT_PAYOUT_SCHEDULE_EXECUTION_FAILED("balancePlatform.balanceAccountPayoutScheduleExecution.failed", CONFIGURATION, true),
    BALANCE_PLATFORM_PAYOUT_SCHEDULE_CREATED("balancePlatform.balancePlatformPayoutSchedule.created", CONFIGURATION, true),
    BALANCE_PLATFORM_PAYOUT_SCHEDULE_UPDATED("balancePlatform.balancePlatformPayoutSchedule.updated", CONFIGURATION, true),
    BALANCE_PLATFORM_PAYOUT_SCHEDULE_DELETED("balancePlatform.balancePlatformPayoutSchedule.deleted", CONFIGURATION, true),
    MANDATE_CREATED("balancePlatform.mandate.created", CONFIGURATION, true),
    MANDATE_UPDATED("balancePlatform.mandate.updated", CONFIGURATION, true),
    SCORE_TRIGGERED("balancePlatform.score.triggered", CONFIGURATION, true);

    private final AdyenWebhookType webhookType;
    private final String eventCodeOrType;
    private final boolean isIgnored;

    AdyenWebhookEvent(String eventCodeOrType, AdyenWebhookType webhookType, boolean isIgnored) {
        this.webhookType = webhookType;
        this.eventCodeOrType = eventCodeOrType;
        this.isIgnored = isIgnored;
    }

    public AdyenWebhookType getWebhookType() {
        return webhookType;
    }

    public String getEventCodeOrType() {
        return eventCodeOrType;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public static Optional<AdyenWebhookEvent> fromEventCodeOrType(String eventCodeOrType) {
        return Arrays.stream(values())
                .filter(event -> event.eventCodeOrType.equals(eventCodeOrType))
                .findFirst();
    }
}
