package uk.gov.pay.connector.gateway.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

@ExtendWith(MockitoExtension.class)
class AdyenGatewayRefundNotificationProcessorTest {
    
    AdyenGatewayRefundNotificationProcessor processor = new AdyenGatewayRefundNotificationProcessor();
    
    @Test
    void shouldReturnTrue_AdyenGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUNDEDAndNewStatusAsREFUND_ERROR() {
        assertThat(processor.isRefundTransitionIllegal(REFUNDED, REFUND_ERROR), is(true));
    }

    @Test
    void shouldReturnFalse_AdyenGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUND_ERRORAndNewStatusAsREFUNDED() {
        assertThat(processor.isRefundTransitionIllegal(REFUND_ERROR, REFUNDED), is(false));
    }

}
