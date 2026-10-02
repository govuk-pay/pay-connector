package uk.gov.pay.connector.gateway.processor;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.defaultGatewayAccountEntity;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.aValidRefundEntity;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

@ExtendWith(MockitoExtension.class)
class RefundNotificationProcessorTest {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(RefundNotificationProcessor.class);

    @Mock
    private RefundService refundService;
    @Mock
    WorldpayGatewayRefundNotificationProcessor worldpayGatewayRefundNotificationProcessor;
    @Mock
    AdyenGatewayRefundNotificationProcessor adyenGatewayRefundNotificationProcessor;
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

        refundNotificationProcessor = new RefundNotificationProcessor(
                refundService,
                worldpayGatewayRefundNotificationProcessor,
                adyenGatewayRefundNotificationProcessor,
                userNotificationService);
    }

    @Test
    void shouldInvokeTransitionRefundStateForSuccessfulRefund() {
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                REFUNDED,
                gatewayAccountEntity,
                charge,
                refundEntity);

        verify(refundService).transitionRefundState(refundEntity, gatewayAccountEntity, REFUNDED, charge);
    }

    @Test
    void shouldInvokeSendEmailNotificationsForSuccessfulRefunds() {
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                REFUNDED,
                gatewayAccountEntity,
                charge,
                refundEntity);

        verify(userNotificationService).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldNotInvokeSendEmailNotifications_WhenRefundStatusIsNotRefunded() {
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                REFUND_ERROR,
                gatewayAccountEntity,
                charge,
                refundEntity);
        
        verify(userNotificationService, never()).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldNotInvokeSendEmailNotifications_WhenRefundStatusWasSetAsRefundError() {
        refundEntity.setStatus(REFUND_ERROR);

        refundNotificationProcessor.invoke(
                paymentGatewayName,
                REFUND_ERROR,
                gatewayAccountEntity,
                charge,
                refundEntity);
        
        verify(userNotificationService, never()).sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
    }

    @Test
    void shouldLogFailedRefund_WhenRefundStatusWasSetAsRefundError() {
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                REFUND_ERROR,
                gatewayAccountEntity,
                charge,
                refundEntity);

        logs.assertContains("Refund request record set as failed (REFUND_ERROR)");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentGatewayName.class, names = {"ADYEN", "WORLDPAY"})
    void shouldLogIllegalStateTransitionAtErrorLevel_WhenRefundTransitionIllegal(PaymentGatewayName gatewayName) {
        RefundStatus currentStatus = REFUNDED;
        RefundStatus newStatus = REFUND_ERROR;
        refundEntity.setStatus(currentStatus);
        
        switch (gatewayName) {
            case ADYEN -> when(adyenGatewayRefundNotificationProcessor.isRefundTransitionIllegal(currentStatus, newStatus))
                    .thenReturn(true);
            case WORLDPAY -> when(worldpayGatewayRefundNotificationProcessor.isRefundTransitionIllegal(currentStatus, newStatus))
                    .thenReturn(true);
            default -> fail("No valid gateway");
        }


        refundNotificationProcessor.invoke(
                gatewayName,
                newStatus,
                gatewayAccountEntity,
                charge,
                refundEntity);

        assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.ERROR))));
        logs.assertContains(gatewayName + " Notification received for refund would cause an illegal state transition");
        
        verifyNoInteractions(refundService);
        verifyNoInteractions(userNotificationService);
    }
    
    @ParameterizedTest
    @EnumSource(RefundStatus.class)
    void shouldNotProcessRefundIfCurrentStatusIsTheSameAsTheNewStatus(RefundStatus status) {
        refundEntity.setStatus(status);
        
        refundNotificationProcessor.invoke(
                paymentGatewayName,
                status,
                gatewayAccountEntity,
                charge,
                refundEntity);

        verifyNoInteractions(refundService, userNotificationService);

        assertThat(logs.getEvents(), everyItem(hasProperty("level", is(Level.INFO))));
        logs.assertContains("Notification received for refund [someExternalId] is redundant and " +
                "therefore ignored because refund is already in state [%s]".formatted(status));
    }

}
