package uk.gov.pay.connector.model.domain;

import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.pay.connector.util.RandomTestDataGeneratorUtils.secureRandomLong;

public class RefundEntityFixture {

    private Long id = secureRandomLong();
    private Long amount = 500L;
    private RefundStatus status = RefundStatus.CREATED;
    private String reference = "reference";
    public static String userExternalId = "AA213FD51B3801043FBC";
    public static String userEmail = "test@example.com";
    private String externalId = "someExternalId";
    private String transactionId = "123456";
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneId.of("UTC"));
    private String chargeExternalId = "chargeExternalId";
    private ZonedDateTime parityCheckDate = null;
    private String paymentProvider;
    private List<Fee> fees;

    public static RefundEntityFixture aValidRefundEntity() {
        return new RefundEntityFixture();
    }

    public RefundEntity build() {
        RefundEntity refundEntity = new RefundEntity(amount, userExternalId, userEmail, chargeExternalId);
        refundEntity.setId(id);
        refundEntity.setStatus(status);
        refundEntity.setExternalId(externalId);
        refundEntity.setUserExternalId(userExternalId);
        refundEntity.setCreatedDate(createdDate);
        refundEntity.setGatewayTransactionId(transactionId);
        refundEntity.setParityCheckDate(parityCheckDate);
        refundEntity.setPaymentProvider(paymentProvider);

        if (this.fees != null) {
            fees.stream().forEach(partialFee -> {
                FeeEntity fee = new FeeEntity(refundEntity, Instant.now(), partialFee);
                refundEntity.addFee(fee);
            });
        }
        
        return refundEntity;
    }

    public RefundEntityFixture withStatus(RefundStatus status) {
        this.status = status;
        return this;
    }

    public RefundEntityFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public RefundEntityFixture withAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public RefundEntityFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public RefundEntityFixture withUserExternalId(String userExternalId) {
        this.userExternalId = userExternalId;
        return this;
    }

    public RefundEntityFixture withChargeExternalId(String chargeExternalId) {
        this.chargeExternalId = chargeExternalId;
        return this;
    }

    public Long getAmount() {
        return amount;
    }

    public RefundEntityFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public RefundEntityFixture withGatewayTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public RefundEntityFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public RefundEntityFixture withParityCheckDate(ZonedDateTime parityCheckDate) {
        this.parityCheckDate = parityCheckDate;
        return this;
    }

    public RefundEntityFixture withPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public RefundEntityFixture withFee(Fee fee) {
        if (this.fees == null) {
            this.fees = new ArrayList<>();
        }
        this.fees.add(fee);
        return this;
    }
}
