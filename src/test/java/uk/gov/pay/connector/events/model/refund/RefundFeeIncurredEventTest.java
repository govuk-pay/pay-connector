package uk.gov.pay.connector.events.model.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.events.exception.EventCreationException;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.model.domain.RefundEntityFixture;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import java.time.Duration;
import java.time.Instant;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.RADAR;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;

public class RefundFeeIncurredEventTest {
    private RefundEntity refundEntity;
    private Instant createdDate;

    @BeforeEach
    void setUp() {
        refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withAmount(100L)
                .withExternalId("refundId")
                .withStatus(REFUNDED)
                .build();

        createdDate = Instant.parse("2026-10-01T10:00:00Z");
    }

    @Test
    void serializesFeeIncurredEventGivenRefundEntity() throws JsonProcessingException, EventCreationException {
        FeeEntity gatewayFee = new FeeEntity(refundEntity, createdDate, Fee.of(GATEWAY, 100L));
        FeeEntity radarFee = new FeeEntity(refundEntity, createdDate.plus(Duration.ofHours(1)), Fee.of(RADAR, 200L));
        refundEntity.addFee(gatewayFee);
        refundEntity.addFee(radarFee);
        String actual = RefundFeeIncurredEvent.from(refundEntity).toJsonString();

        assertThat(actual, hasJsonPath("$.timestamp", equalTo("2026-10-01T10:00:00.000000Z")));
        assertThat(actual, hasJsonPath("$.event_type", equalTo("REFUND_FEE_INCURRED_EVENT")));
        assertThat(actual, hasJsonPath("$.resource_type", equalTo("refund")));
        assertThat(actual, hasJsonPath("$.resource_external_id", equalTo("refundId")));
        assertThat(actual, hasJsonPath("$.event_details.fee", equalTo(300)));
        assertThat(actual, hasJsonPath("$.event_details.net_amount", equalTo(-400)));
        assertThat(actual, hasJsonPath("$.event_details.fee_breakdown[0].fee_type", equalTo("gateway")));
        assertThat(actual, hasJsonPath("$.event_details.fee_breakdown[0].amount", equalTo(100)));
        assertThat(actual, hasNoJsonPath("$.event_details.fee_breakdown[0].fee_sub_type"));
        assertThat(actual, hasJsonPath("$.event_details.fee_breakdown[1].fee_type", equalTo("radar")));
        assertThat(actual, hasJsonPath("$.event_details.fee_breakdown[1].amount", equalTo(200)));
        assertThat(actual, hasNoJsonPath("$.event_details.fee_breakdown[1].fee_sub_type"));
    }

}
