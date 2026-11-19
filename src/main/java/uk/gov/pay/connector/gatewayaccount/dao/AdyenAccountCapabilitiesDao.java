package uk.gov.pay.connector.gatewayaccount.dao;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import uk.gov.pay.connector.common.dao.JpaDao;
import uk.gov.pay.connector.gatewayaccount.model.AdyenAccountCapabilitiesEntity;

import java.util.Optional;

public class AdyenAccountCapabilitiesDao extends JpaDao<AdyenAccountCapabilitiesEntity> {

    @Inject
    public AdyenAccountCapabilitiesDao(Provider<EntityManager> entityManager) {
        super(entityManager);
    }
    
    public Optional<AdyenAccountCapabilitiesEntity> findByGatewayAccountId(Long gatewayAccountId) {
        String query = "SELECT ac FROM AdyenAccountCapabilitiesEntity ac WHERE ac.gatewayAccount.id = :gatewayAccountId";
        
        return entityManager.get()
                .createQuery(query, AdyenAccountCapabilitiesEntity.class)
                .setParameter("gatewayAccountId", gatewayAccountId)
                .setMaxResults(1)
                .getResultList()
                .stream().findFirst();
    }
}
