package uk.gov.pay.connector.model;

import com.jayway.jsonassert.JsonAssert;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeType;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountType;
import uk.gov.pay.connector.model.domain.RefundEntityFixture;
import uk.gov.pay.connector.refund.model.RefundResponse;
import uk.gov.pay.connector.refund.model.RefundsResponse;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

@ExtendWith(MockitoExtension.class)
class RefundResponseTest {

    @Mock
    private UriInfo mockUriInfo;

    @Test
    void shouldSerializeARefundWithExpectedFieldsDefined() {

        // given
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();
        RefundEntity refund = RefundEntityFixture.aValidRefundEntity()
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();

        String chargeId = refund.getChargeExternalId();
        String refundId = refund.getExternalId();

        String expectedPaymentLink = "http://app.com/v1/api/accounts/1/charges/" + chargeId;
        String expectedSelfLink = expectedPaymentLink + "/refunds/" + refundId;

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com"));

        // when
        String serializedResponse = RefundResponse.valueOf(refund, chargeEntity.getGatewayAccount().getId(), mockUriInfo).serialize();

        // then
        JsonAssert.with(serializedResponse)
                .assertThat("$.amount", is(500))
                .assertThat("$.status", is("submitted"))
                .assertThat("$._links.self.href", is(expectedSelfLink))
                .assertThat("$._links.payment.href", is(expectedPaymentLink))
                .assertThat("$.refund_id", is(refundId))
                .assertNotNull("$.created_date");
    }

    @Test
    void shouldSerializeAListOfRefundsWithExpectedFieldsDefined() {

        // given
        RefundEntity refund1 = RefundEntityFixture.aValidRefundEntity().withAmount(10L).withStatus(REFUNDED).build();
        RefundEntity refund2 = RefundEntityFixture.aValidRefundEntity().withAmount(20L).withStatus(RefundStatus.CREATED).build();

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();

        String chargeId = chargeEntity.getExternalId();
        String refundIdRefund1 = refund1.getExternalId();
        String refundIdRefund2 = refund2.getExternalId();

        String expectedPaymentLink = "http://app.com/v1/api/accounts/1/charges/" + chargeId;
        String expectedSelfLink = expectedPaymentLink + "/refunds";

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com"), UriBuilder.fromUri("http://app.com"), UriBuilder.fromUri("http://app.com"), UriBuilder.fromUri("http://app.com"), UriBuilder.fromUri("http://app.com"));

        // when
        String serializedResponse = RefundsResponse.valueOf(chargeEntity, List.of(refund1, refund2), mockUriInfo).serialize();

        // then
        JsonAssert.with(serializedResponse)
                .assertThat("$.payment_id", is(chargeId))
                .assertThat("$._links.self.href", is(expectedSelfLink))
                .assertThat("$._links.payment.href", is(expectedPaymentLink))
                .assertThat("_embedded.refunds[0].refund_id", is(refundIdRefund1))
                .assertThat("_embedded.refunds[0].amount", is(10))
                .assertThat("_embedded.refunds[0].status", is("success"))
                .assertThat("_embedded.refunds[0].created_date", is(notNullValue()))
                .assertThat("_embedded.refunds[0]._links.self.href", is(notNullValue()))
                .assertThat("_embedded.refunds[0]._links.payment.href", is(notNullValue()))
                .assertThat("_embedded.refunds[1].refund_id", is(refundIdRefund2))
                .assertThat("_embedded.refunds[1].amount", is(20))
                .assertThat("_embedded.refunds[1].status", is("submitted"))
                .assertThat("_embedded.refunds[1].created_date", is(notNullValue()))
                .assertThat("_embedded.refunds[1]._links.self.href", is(notNullValue()))
                .assertThat("_embedded.refunds[1]._links.payment.href", is(notNullValue()));

    }

    @Test
    void shouldSerialiseFeeAndNetAmountWhenRefundHasFeeForSuccessfulRefund() {

        // given
        ChargeEntity chargeEntity = ChargeEntityFixture
                .aValidChargeEntity()
                .build();

        RefundEntity refund = RefundEntityFixture
                .aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUNDED)
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();

        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 2L));

        refund.getFees().add(feeEntity);

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com"));

        // when
        String serializedResponse = RefundResponse.valueOf(refund, chargeEntity.getGatewayAccount().getId(), 
                mockUriInfo).serialize();

        // then
        JsonAssert.with(serializedResponse).assertThat("$.fee", is(2))
                .assertThat("$.net_amount", is(-102));
    }

    @Test
    void shouldSerialiseFeeAndNetAmountRefundWhenServiceBasedForSuccessfulRefund() {

        // given
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();

        RefundEntity refund = RefundEntityFixture
                .aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUNDED)
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();

        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 2L));

        refund.getFees().add(feeEntity);

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(
                UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com")
        );

        // when
        String serializedResponse = RefundResponse.valueOf(refund, "service-id", GatewayAccountType.TEST, 
                mockUriInfo).serialize();

        // then
        JsonAssert
                .with(serializedResponse)
                .assertThat("$.fee", is(2))
                .assertThat("$.net_amount", is(-102));
    }

    @Test
    void shouldNotSerialiseFeeAndNetAmountWhenRefundHasNoFee() {

        // given
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();

        RefundEntity refund = RefundEntityFixture
                .aValidRefundEntity()
                .withAmount(100L)
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com"));

        // when
        String serializedResponse = RefundResponse.valueOf(refund, chargeEntity.getGatewayAccount().getId(), 
                mockUriInfo).serialize();

        // then
        JsonAssert.
                with(serializedResponse)
                .assertNotDefined("$.fee")
                .assertNotDefined("$.net_amount");
    }

    @Test
    void shouldSerialiseFeeAndNetAmountForFailedRefund() {

        // given
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();

        RefundEntity refund = RefundEntityFixture
                .aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUND_ERROR)
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();

        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 2L));

        refund.getFees().add(feeEntity);

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(
                UriBuilder.fromUri("http://app.com"),
                UriBuilder.fromUri("http://app.com")
        );

        // when
        String serializedResponse = RefundResponse.valueOf(refund, "service-id", GatewayAccountType.TEST,
                mockUriInfo).serialize();

        // then
        JsonAssert
                .with(serializedResponse)
                .assertThat("$.fee", is(2))
                .assertThat("$.net_amount", is(-2));
    }
    
}
