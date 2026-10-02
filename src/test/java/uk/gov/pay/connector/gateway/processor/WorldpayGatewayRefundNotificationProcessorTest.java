package uk.gov.pay.connector.gateway.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

@ExtendWith(MockitoExtension.class)
class WorldpayGatewayRefundNotificationProcessorTest {
    
    WorldpayGatewayRefundNotificationProcessor processor = new WorldpayGatewayRefundNotificationProcessor();

    @Test
    void shouldReturnTrue_WorldpayGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUNDEDAndNewStatusAsREFUND_ERROR() {
        assertThat(processor.isRefundTransitionIllegal(REFUNDED, REFUND_ERROR), is(true));
    }

    @Test
    void shouldReturnTrue_WorldpayGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUND_ERRORAndNewStatusAsREFUNDED() {
        assertThat(processor.isRefundTransitionIllegal(REFUND_ERROR, REFUNDED), is(true));
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED, REFUNDED",
            "REFUND_SUBMITTED, REFUNDED",
            "REFUND_SUBMITTED, REFUND_ERROR"
    })
    void shouldReturnFalse_WorldpayGatewayRefundNotificationProcessorTest_CalledWithoutREFUNDEDAndREFUND_ERROR(RefundStatus currentStatus, RefundStatus newStatus) {
        assertThat(processor.isRefundTransitionIllegal(currentStatus, newStatus), is(false));
    }
}
