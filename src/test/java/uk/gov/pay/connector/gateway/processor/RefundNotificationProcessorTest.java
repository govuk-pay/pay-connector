package uk.gov.pay.connector.gateway.processor;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.pay.connector.charge.model.ServicePaymentReference;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.RefundService;
import uk.gov.pay.connector.usernotification.service.UserNotificationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.defaultGatewayAccountEntity;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.WORLDPAY;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.aValidRefundEntity;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

@ExtendWith(MockitoExtension.class)
class RefundNotificationProcessorTest {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(RefundNotificationProcessor.class);

    @Mock
    private RefundService refundService;
    @Mock
    private UserNotificationService userNotificationService;

    RefundNotificationProcessor refundNotificationProcessor;
    RefundEntity refundEntity;

    private static PaymentGatewayName paymentGatewayName = PaymentGatewayName.WORLDPAY;
    private static final String PAYMENT_REFERENCE = "payment-reference";
    private static final String TRANSACTION_ID = "transactionId";
    private final GatewayAccountEntity gatewayAccountEntity = defaultGatewayAccountEntity();
    private final ChargeEntity chargeEntity = aValidChargeEntity()
            .withGatewayAccountEntity(gatewayAccountEntity)
            .withReference(ServicePaymentReference.of(PAYMENT_REFERENCE))
            .withTransactionId(TRANSACTION_ID)
            .build();
    private Charge charge;

    @BeforeEach
    void setup() {
        charge = Charge.from(chargeEntity);
        refundEntity = aValidRefundEntity().build();

        refundNotificationProcessor = new RefundNotificationProcessor(refundService, userNotificationService);
    }

    @Test
    void shouldInvokeTransitionRefundStateForSuccessfulRefund() {
        var targetRefundStatus = RefundStatus.REFUNDED;

        invokeRefundNotificationProcessorWithNewStatus(targetRefundStatus);

        verify(refundService)
                .transitionRefundState(refundEntity, gatewayAccountEntity, targetRefundStatus, charge);
    }

    @Test
    void shouldInvokeSendEmailNotificationsForSuccessfulRefunds() {
        invokeRefundNotificationProcessorWithNewStatus(RefundStatus.REFUNDED);
        verify(userNotificationService).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldNotInvokeSendEmailNotifications_WhenRefundStatusIsNotRefunded() {
        invokeRefundNotificationProcessorWithNewStatus(REFUND_ERROR);
        verify(userNotificationService, never()).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldNotInvokeSendEmailNotifications_WhenRefundStatusWasSetAsRefundError() {
        refundEntity.setStatus(REFUND_ERROR);

        invokeRefundNotificationProcessorWithNewStatus(REFUND_ERROR);
        verify(userNotificationService, never()).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldLogFailedRefund_WhenRefundStatusWasSetAsRefundError() {
        invokeRefundNotificationProcessorWithNewStatus(REFUND_ERROR);

        logs.assertContains("Refund request record set as failed (REFUND_ERROR)");
    }


    @Test
    void shouldLogIllegalStateTransitionAtInfoLevel_WhenWorldpayStatusTransitionIsIllegal() {
        refundEntity.setStatus(RefundStatus.REFUND_ERROR);

        invokeRefundNotificationProcessorWithNewStatus(RefundStatus.REFUNDED);

        assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.INFO))));
        logs.assertContains("Notification received for refund would cause an illegal state transition");
        then(refundService)
                .should(never())
                .transitionRefundState(any(), any(), any(), any());
        then(userNotificationService)
                .should(never())
                .sendRefundIssuedEmail(any(), any(), any());
    }

    @Nested
    @ParameterizedClass
    @CsvSource({
            "REFUNDED, REFUND_ERROR",
            "REFUND_ERROR, REFUNDED"
    })
    class WorldpayLogInfoWhenStatusTransitionIsIllegal {

        @Parameter(0)
        RefundStatus oldStatus;
        @Parameter(1)
        RefundStatus newStatus;


        @BeforeEach
        void setUp() {
            paymentGatewayName = WORLDPAY;
            refundEntity.setStatus(oldStatus);
        }

        @Test
        void shouldNotTransitionTheRefundState() {
            invokeRefundNotificationProcessorWithNewStatus(newStatus);

            then(refundService)
                    .should(never())
                    .transitionRefundState(any(), any(), any(), any());
        }

        @Test
        void shouldNotSendRefundIssuedEmail() {
            invokeRefundNotificationProcessorWithNewStatus(newStatus);

            then(userNotificationService)
                    .should(never())
                    .sendRefundIssuedEmail(any(), any(), any());
        }

        @Test
        void shouldLogIllegalStateTransitionAtInfoLevel() {
            invokeRefundNotificationProcessorWithNewStatus(newStatus);

            assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.INFO))));
            logs.assertContains("Notification received for refund would cause an illegal state " +
                    "transition: refund [%s] cannot be set as [%s] because it is already in state [%s].".formatted(
                            refundEntity.getExternalId(), newStatus, oldStatus));
        }
    }


    @Nested
    @ParameterizedClass
    @EnumSource(RefundStatus.class)
    class WhenOldStatusIsTheSameAsTheNewStatus {

        @Parameter
        RefundStatus status;

        @BeforeEach
        void setUp() {
            refundEntity.setStatus(status);
        }

        @Test
        void shouldNotTransitionTheRefundState() {
            invokeRefundNotificationProcessorWithNewStatus(status);

            then(refundService)
                    .should(never())
                    .transitionRefundState(any(), any(), any(), any());

        }

        @Test
        void shouldNotSendRefundIssuedEmail() {
            invokeRefundNotificationProcessorWithNewStatus(status);

            then(userNotificationService)
                    .should(never())
                    .sendRefundIssuedEmail(any(), any(), any());
        }

        @Test
        void shouldLogRedundantNotificationMessageAtInfoLevel() {
            invokeRefundNotificationProcessorWithNewStatus(status);

            assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.INFO))));
            logs.assertContains("Notification received for refund [someExternalId] is redundant and " +
                    "therefore ignored because refund is already in state [%s]".formatted(status));
        }
    }

    private void invokeRefundNotificationProcessorWithNewStatus(RefundStatus newStatus) {
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                newStatus,
                gatewayAccountEntity,
                charge,
                refundEntity);
    }

    @Test
    void shouldTransitionRefund_WhenRefundStatusWasSetAsRefundError_ForAdyen() {
        refundEntity.setStatus(REFUND_ERROR);
        invokeRefundNotificationProcessor(ADYEN, RefundStatus.REFUNDED);

        verify(refundService)
                .transitionRefundState(refundEntity, gatewayAccountEntity, RefundStatus.REFUNDED, charge);
        verify(userNotificationService).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    

    @Test
    void shouldLogIllegalStateTransitionAtErrorLevel_IfRefundFailedWhenRefundStatusWasSetAsRefundedForAdyen() {
        refundEntity.setStatus(RefundStatus.REFUNDED);

        invokeRefundNotificationProcessor(ADYEN, RefundStatus.REFUND_ERROR);

        assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.ERROR))));
        logs.assertContains("Adyen Notification received for refund would cause an illegal state transition");
        then(refundService)
                .should(never())
                .transitionRefundState(any(), any(), any(), any());
        then(userNotificationService)
                .should(never())
                .sendRefundIssuedEmail(any(), any(), any());
    }

    @Test
    void shouldLogRedundantNotificationAtInfoLevel_WhenStatusIsUnchangedUsingExternalId() {
        refundEntity.setStatus(RefundStatus.REFUNDED);

        invokeRefundNotificationProcessor(ADYEN, RefundStatus.REFUNDED);

        assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.INFO))));
        logs.assertContains("Notification received for refund [someExternalId] is redundant and therefore ignored because refund is already in state [REFUNDED]");
        then(refundService)
                .should(never())
                .transitionRefundState(any(), any(), any(), any());
        then(userNotificationService)
                .should(never())
                .sendRefundIssuedEmail(any(), any(), any());
    }

  
    private void invokeRefundNotificationProcessor(PaymentGatewayName gatewayName, RefundStatus newStatus) {
        refundNotificationProcessor.invoke(
                gatewayName,
                newStatus,
                gatewayAccountEntity,
                charge,
                refundEntity);
    }
}
