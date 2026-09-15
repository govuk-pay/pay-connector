package uk.gov.pay.connector.payout;

import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.Event;
import uk.gov.pay.connector.events.model.charge.PaymentIncludedInPayout;
import uk.gov.pay.connector.events.model.dispute.DisputeIncludedInPayout;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.events.model.payout.PayoutEvent;
import uk.gov.pay.connector.events.model.refund.RefundIncludedInPayout;
import uk.gov.pay.connector.gateway.stripe.StripeSdkClient;
import uk.gov.pay.connector.gateway.stripe.json.StripePayoutStatus;
import uk.gov.pay.connector.gateway.stripe.request.StripeTransferMetadata;
import uk.gov.pay.connector.gateway.stripe.request.StripeTransferMetadataReason;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.StripeCredentials;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.queue.payout.PayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.StripePayoutReconciliationPayload;
import uk.gov.pay.connector.util.MDCUtils;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.CONNECT_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.DISPUTE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_PAYOUT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.REFUND_EXTERNAL_ID;

public class StripePayoutReconciler implements PayoutReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripePayoutReconciler.class);
    private final StripeSdkClient stripeClient;
    private final ConnectorConfiguration connectorConfiguration;
    private final GatewayAccountCredentialsService gatewayAccountCredentialsService;
    private final EventService eventService;
    private final PayoutEmitterService payoutEmitterService;

    public StripePayoutReconciler(StripeSdkClient stripeClient,
                                  ConnectorConfiguration connectorConfiguration,
                                  GatewayAccountCredentialsService gatewayAccountCredentialsService,
                                  EventService eventService,
                                  PayoutEmitterService payoutEmitterService) {
        this.stripeClient = stripeClient;
        this.connectorConfiguration = connectorConfiguration;
        this.gatewayAccountCredentialsService = gatewayAccountCredentialsService;
        this.eventService = eventService;
        this.payoutEmitterService = payoutEmitterService;
    }

    @Override
    public void reconcile(PayoutReconciliationPayload payout) throws QueueException {
        if (!(payout instanceof StripePayoutReconciliationPayload)) {
            LOGGER.error("Invalid payout type for Stripe reconciler: {}", payout.getClass().getSimpleName());
            return;
        }

        StripePayoutReconciliationPayload stripePayout = (StripePayoutReconciliationPayload) payout;
        processStripePayout(stripePayout);
    }

    private void processStripePayout(StripePayoutReconciliationPayload payout) throws QueueException {
        MDC.put(GATEWAY_PAYOUT_ID, payout.getGatewayPayoutId());
        MDC.put(CONNECT_ACCOUNT_ID, payout.getConnectAccountId());

        GatewayAccountEntity gatewayAccountEntity = gatewayAccountCredentialsService
                .findStripeGatewayAccountForCredentialKeyAndValue(StripeCredentials.STRIPE_ACCOUNT_ID_KEY, payout.getConnectAccountId());
        MDCUtils.addGatewayAccountDetailsToMDC(gatewayAccountEntity);

        LOGGER.info("Processing Stripe payout [{}] for connect account [{}]",
                payout.getGatewayPayoutId(),
                payout.getConnectAccountId());

        AtomicInteger payments = new AtomicInteger();
        AtomicInteger transfers = new AtomicInteger();

        Iterable<BalanceTransaction> balanceTransactions;
        try {
            balanceTransactions = stripeClient.getBalanceTransactionsForPayout(
                    payout.getGatewayPayoutId(), payout.getConnectAccountId(),
                    gatewayAccountEntity.isLive());
        } catch (com.stripe.exception.StripeException e) {
            LOGGER.error("Error fetching balance transactions for Stripe payout [{}]: {}",
                    payout.getGatewayPayoutId(), e.getMessage());
            throw new QueueException("Failed to fetch balance transactions for Stripe payout: " + e.getMessage());
        }

        balanceTransactions.forEach(balanceTransaction -> {
            switch (balanceTransaction.getType()) {
                case "payment":
                    reconcilePayment(payout, balanceTransaction);
                    payments.getAndIncrement();
                    break;
                case "transfer":
                    reconcileTransfer(payout, balanceTransaction);
                    transfers.getAndIncrement();
                    break;
                case "payout":
                    emitPayoutCreatedEvent(payout, balanceTransaction);
                    break;
                default:
                    LOGGER.error(format("Payout contains balance transfer of type [%s], which is unexpected.",
                            balanceTransaction.getType()));
                    break;
            }
        });

        if (payments.intValue() == 0 && transfers.intValue() == 0) {
            LOGGER.error("No payments or refunds retrieved for payout [{}]. Requires investigation.",
                    payout.getGatewayPayoutId());
        } else {
            LOGGER.info("Finished processing Stripe payout [{}]. Emitted events for {} payments and {} transfers.",
                    payout.getGatewayPayoutId(),
                    payments.intValue(),
                    transfers.intValue());
        }

        MDC.remove(GATEWAY_PAYOUT_ID);
        MDC.remove(CONNECT_ACCOUNT_ID);
        MDCUtils.removeGatewayAccountDetailsFromMDC();
    }

    private void emitPayoutCreatedEvent(StripePayoutReconciliationPayload payout, BalanceTransaction balanceTransaction) {
        com.stripe.model.Payout payoutObject = (com.stripe.model.Payout) balanceTransaction.getSourceObject();
        uk.gov.pay.connector.gateway.stripe.json.StripePayout stripePayout = uk.gov.pay.connector.gateway.stripe.json.StripePayout.from(payoutObject);

        payoutEmitterService.emitPayoutEvent(PayoutCreated.class, stripePayout.getCreated(),
                payout.getConnectAccountId(), stripePayout);

        emitTerminalPayoutEvent(payout.getConnectAccountId(), stripePayout);
    }

    private void emitTerminalPayoutEvent(String connectAccountId, uk.gov.pay.connector.gateway.stripe.json.StripePayout stripePayout) {
        StripePayoutStatus stripePayoutStatus = StripePayoutStatus.fromString(stripePayout.getStatus());
        if (stripePayoutStatus.isTerminal()) {
            java.util.Optional<Class<? extends PayoutEvent>> mayBeEventClass = stripePayoutStatus.getEventClass();
            mayBeEventClass.ifPresentOrElse(
                    eventClass -> payoutEmitterService.emitPayoutEvent(eventClass, stripePayout.getCreated(),
                            connectAccountId, stripePayout),
                    () -> LOGGER.warn("Event class is not available for a payout in terminal status. " +
                                    "gateway_payout_id [{}], connect_account_id [{}], status [{}]",
                            stripePayout.getId(), connectAccountId, stripePayout.getStatus())
            );
        }
    }

    private void reconcilePayment(StripePayoutReconciliationPayload payout, BalanceTransaction balanceTransaction) {
        var paymentSource = (Charge) balanceTransaction.getSourceObject();
        var paymentSourceTransfer = paymentSource.getSourceTransferObject();
        StripeTransferMetadata stripeTransferMetadata = getStripeTransferMetadata(paymentSourceTransfer);
        String paymentExternalId = resolveTransactionExternalId(payout, balanceTransaction, stripeTransferMetadata);

        emitPaymentEvent(payout, paymentExternalId);
    }

    private void reconcileTransfer(StripePayoutReconciliationPayload payout, BalanceTransaction balanceTransaction) {
        var sourceTransfer = (Transfer) balanceTransaction.getSourceObject();
        var stripeTransferMetadata = getStripeTransferMetadata(sourceTransfer);
        String transactionExternalId = resolveTransactionExternalId(payout, balanceTransaction, stripeTransferMetadata);

        StripeTransferMetadataReason reason = stripeTransferMetadata.getReason();
        switch (reason) {
            case TRANSFER_FEE_AMOUNT_FOR_FAILED_PAYMENT:
                emitPaymentEvent(payout, transactionExternalId);
                break;
            case TRANSFER_REFUND_AMOUNT:
            case NOT_DEFINED:
                emitRefundEvent(payout, transactionExternalId);
                break;
            case TRANSFER_DISPUTE_AMOUNT:
                emitDisputeEvent(payout, transactionExternalId);
                break;
            default:
                throw new RuntimeException(String.format("Stripe balance transaction %s has unexpected 'reason' in metadata", balanceTransaction.getId()));
        }
    }

    private StripeTransferMetadata getStripeTransferMetadata(Transfer sourceTransfer) {
        return StripeTransferMetadata.from(sourceTransfer.getMetadata());
    }

    private void emitPaymentEvent(StripePayoutReconciliationPayload payout, String paymentExternalId) {
        var paymentEvent = new PaymentIncludedInPayout(paymentExternalId,
                payout.getGatewayPayoutId(),
                payout.getCreatedDate());
        emitEvent(paymentEvent, payout, paymentExternalId);

        LOGGER.info(format("Emitted event for payment [%s] included in payout [%s]",
                        paymentExternalId,
                        payout.getGatewayPayoutId()),
                kv(PAYMENT_EXTERNAL_ID, paymentExternalId));
    }

    private void emitRefundEvent(StripePayoutReconciliationPayload payout, String refundExternalId) {
        var refundEvent = new RefundIncludedInPayout(refundExternalId,
                payout.getGatewayPayoutId(),
                payout.getCreatedDate());
        emitEvent(refundEvent, payout, refundExternalId);

        LOGGER.info(format("Emitted event for refund [%s] included in payout [%s]",
                        refundExternalId,
                        payout.getGatewayPayoutId()),
                kv(REFUND_EXTERNAL_ID, refundExternalId));
    }

    private void emitDisputeEvent(StripePayoutReconciliationPayload payout, String disputeExternalId) {
        var disputeEvent = new DisputeIncludedInPayout(disputeExternalId,
                payout.getGatewayPayoutId(),
                payout.getCreatedDate());
        emitEvent(disputeEvent, payout, disputeExternalId);

        LOGGER.info(format("Emitted event for dispute [%s] included in payout [%s]",
                        disputeExternalId,
                        payout.getGatewayPayoutId()),
                kv(DISPUTE_EXTERNAL_ID, disputeExternalId));
    }

    private String resolveTransactionExternalId(StripePayoutReconciliationPayload payout, BalanceTransaction balanceTransaction, StripeTransferMetadata stripeTransferMetadata) {
        var transactionExternalId = stripeTransferMetadata.getGovukPayTransactionExternalId();
        if (transactionExternalId == null) {
            throw new RuntimeException(format("Transaction external ID missing in metadata on Stripe transfer for " +
                            "balance transaction [%s] for payout [%s] for gateway account [%s]",
                    balanceTransaction.getId(), payout.getGatewayPayoutId(),
                    payout.getConnectAccountId()));
        }
        return transactionExternalId;
    }

    private void emitEvent(Event event, StripePayoutReconciliationPayload payout, String transactionExternalId) {
        if (TRUE.equals(connectorConfiguration.getEmitPayoutEvents())) {
            try {
                eventService.emitEvent(event, false);
            } catch (QueueException e) {
                throw new RuntimeException(format("Error sending %s event for transaction [%s] included in payout [%s] to event queue: %s",
                        event.getEventType(), transactionExternalId, payout.getGatewayPayoutId(), e.getMessage()), e);
            }
        }
    }
}
