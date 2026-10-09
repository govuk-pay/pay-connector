package uk.gov.pay.connector.fee.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import uk.gov.pay.connector.charge.model.domain.FeeProcessingLockEntity;
import uk.gov.pay.connector.common.dao.JpaDao;

import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Transactional
public class FeeProcessingLockDao extends JpaDao<FeeProcessingLockEntity> {

    @Inject
    public FeeProcessingLockDao(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    public boolean insertIfAbsent(Long chargeId) {
        return entityManager.get().createNativeQuery("""
                            INSERT INTO fee_processing_lock (charge_id)
                            VALUES (?)
                            ON CONFLICT (charge_id) DO NOTHING
                        """)
                .setParameter(1, chargeId)
                .executeUpdate() == 1;
    }

    public Optional<FeeProcessingLockEntity> lockForProcessing(Long chargeId) {
        entityManager.get().createNativeQuery("SET LOCAL lock_timeout='5s'")
                .executeUpdate();

        TypedQuery<FeeProcessingLockEntity> query = entityManager.get()
                .createQuery(
                        "SELECT fpl FROM FeeProcessingLockEntity fpl WHERE fpl.chargeEntity.id = :chargeId",
                        FeeProcessingLockEntity.class)
                .setParameter("chargeId", chargeId)
                .setLockMode(PESSIMISTIC_WRITE);

        return query.getResultStream()
                .findFirst();
    }
}
