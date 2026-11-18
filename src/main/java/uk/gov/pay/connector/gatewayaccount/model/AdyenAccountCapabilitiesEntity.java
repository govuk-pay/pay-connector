package uk.gov.pay.connector.gatewayaccount.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import uk.gov.pay.connector.gatewayaccountcredentials.model.GatewayAccountCredentialsEntity;

@Entity
@Table(name = "gateway_accounts_adyen_capabilities")
@SequenceGenerator(name = "gateway_accounts_adyen_capabilities_id_seq",
        sequenceName = "gateway_accounts_adyen_capabilities_id_seq", allocationSize = 1)
public class AdyenAccountCapabilitiesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gateway_accounts_adyen_capabilities_id_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gateway_account_id", updatable = false)
    private GatewayAccountEntity gatewayAccount;

    @ManyToOne
    @JoinColumn(name = "gateway_account_credential_id", updatable = false)
    private GatewayAccountCredentialsEntity gatewayAccountCredential;

    @Column(name = "receive_from_platform_payments")
    private Boolean receiveFromPlatformAccount;

    @Column(name = "receive_from_balance_account")
    private Boolean receiveFromBalanceAccount;

    @Column(name = "send_to_balance_account")
    private Boolean sendToBalanceAccount;

    @Column(name = "send_to_transfer_instrument")
    private Boolean sendToTransferInstrument;

    @Column(name = "receive_payments")
    private Boolean receivePayments;

    public AdyenAccountCapabilitiesEntity(Long id, GatewayAccountEntity gatewayAccount, 
                                          GatewayAccountCredentialsEntity gatewayAccountCredential,
                                          Boolean receiveFromPlatformAccount,
                                          Boolean receiveFromBalanceAccount, Boolean sendToBalanceAccount,
                                          Boolean sendToTransferInstrument, Boolean receivePayments) {
        this.id = id;
        this.gatewayAccount = gatewayAccount;
        this.gatewayAccountCredential = gatewayAccountCredential;
        this.receiveFromPlatformAccount = receiveFromPlatformAccount;
        this.receiveFromBalanceAccount = receiveFromBalanceAccount;
        this.sendToBalanceAccount = sendToBalanceAccount;
        this.sendToTransferInstrument = sendToTransferInstrument;
        this.receivePayments = receivePayments;
    }

    public AdyenAccountCapabilitiesEntity() {
        // We ❤️ JPA
    }
    
    public Long getId() {
        return id;
    }

    public Boolean isReceivePayments() {
        return receivePayments;
    }

    public Boolean isSendToTransferInstrument() {
        return sendToTransferInstrument;
    }

    public Boolean isSendToBalanceAccount() {
        return sendToBalanceAccount;
    }

    public Boolean isReceiveFromBalanceAccount() {
        return receiveFromBalanceAccount;
    }

    public Boolean isReceiveFromPlatformAccount() {
        return receiveFromPlatformAccount;
    }

    public GatewayAccountCredentialsEntity getGatewayAccountCredential() {
        return gatewayAccountCredential;
    }

    public GatewayAccountEntity getGatewayAccount() {
        return gatewayAccount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReceivePayments(Boolean receivePayments) {
        this.receivePayments = receivePayments;
    }

    public void setSendToTransferInstrument(Boolean sendToTransferInstrument) {
        this.sendToTransferInstrument = sendToTransferInstrument;
    }

    public void setSendToBalanceAccount(Boolean sendToBalanceAccount) {
        this.sendToBalanceAccount = sendToBalanceAccount;
    }

    public void setReceiveFromBalanceAccount(Boolean receiveFromBalanceAccount) {
        this.receiveFromBalanceAccount = receiveFromBalanceAccount;
    }

    public void setReceiveFromPlatformAccount(Boolean receiveFromPlatformAccount) {
        this.receiveFromPlatformAccount = receiveFromPlatformAccount;
    }

    public void setGatewayAccountCredential(GatewayAccountCredentialsEntity gatewayAccountCredential) {
        this.gatewayAccountCredential = gatewayAccountCredential;
    }

    public void setGatewayAccount(GatewayAccountEntity gatewayAccount) {
        this.gatewayAccount = gatewayAccount;
    }
}
