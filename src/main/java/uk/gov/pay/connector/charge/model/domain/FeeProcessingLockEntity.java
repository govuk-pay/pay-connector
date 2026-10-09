package uk.gov.pay.connector.charge.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fee_processing_lock")
@Access(AccessType.FIELD)
public class FeeProcessingLockEntity {

    public FeeProcessingLockEntity() {
    }

    public FeeProcessingLockEntity(ChargeEntity chargeEntity) {
        this.chargeEntity = chargeEntity;
    }

    @Id
    private long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "charge_id", updatable = false)
    private ChargeEntity chargeEntity;

    public ChargeEntity getChargeEntity() {
        return chargeEntity;
    }
}
