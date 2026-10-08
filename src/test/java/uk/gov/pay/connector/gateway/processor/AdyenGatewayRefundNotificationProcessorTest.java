package uk.gov.pay.connector.gateway.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.charge.RefundAvailabilityUpdated;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_SUBMITTED;

@ExtendWith(MockitoExtension.class)
class AdyenGatewayRefundNotificationProcessorTest {
    private static final Instant INSTANT = Instant.parse("2026-10-02T10:24:16Z");

    @Mock
    ChargeService mockChargeService;
    @Mock
    EventService mockEventService;
    @Mock
    Charge mockCharge;
    @Mock
    RefundAvailabilityUpdated mockRefundAvailabilityUpdated;
    
    AdyenGatewayRefundNotificationProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new AdyenGatewayRefundNotificationProcessor(mockChargeService, mockEventService);
    }
    
    @Test
    void shouldReturnTrue_AdyenGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUNDEDAndNewStatusAsREFUND_ERROR() {
        assertThat(processor.isRefundTransitionIllegal(REFUNDED, REFUND_ERROR), is(true));
    }

    @Test
    void shouldReturnFalse_AdyenGatewayRefundNotificationProcessorTest_CalledWithCurrentStatusREFUND_ERRORAndNewStatusAsREFUNDED() {
        assertThat(processor.isRefundTransitionIllegal(REFUND_ERROR, REFUNDED), is(false));
    }
    
    @Test
    void shouldEmitEvent_WhenOldStatusIsREFUND_ERRORAndNewStatusIsREFUNDED() {
        when(mockChargeService.createRefundAvailabilityUpdatedEvent(mockCharge, INSTANT)).thenReturn(mockRefundAvailabilityUpdated);
        
        processor.emitAdditionalEvents(mockCharge, REFUND_ERROR, REFUNDED, INSTANT);
        
        verify(mockEventService).emitAndRecordEvent(mockRefundAvailabilityUpdated);
    }

    @Test
    void shouldNotEmitEvent_WhenOldStatusIsSUBMITTEDAndNewStatusIsREFUNDED() {
        processor.emitAdditionalEvents(mockCharge, REFUND_SUBMITTED, REFUNDED, INSTANT);

        verifyNoInteractions(mockEventService);
    }

}
