package uk.gov.pay.connector.charge.model.domain;

import uk.gov.pay.connector.cardtype.model.domain.CardType;
import uk.gov.pay.connector.charge.model.AddressEntity;
import uk.gov.pay.connector.charge.model.CardDetailsEntity;
import uk.gov.pay.connector.charge.model.FirstDigitsCardNumber;
import uk.gov.pay.connector.charge.model.LastDigitsCardNumber;
import uk.gov.pay.connector.model.domain.AddressFixture;
import uk.gov.service.payments.commons.model.CardExpiryDate;

public class CardDetailsEntityFixture {

    private FirstDigitsCardNumber firstDigitsCardNumber = FirstDigitsCardNumber.of("424242");
    private LastDigitsCardNumber lastDigitsCardNumber = LastDigitsCardNumber.of("4242");
    private String cardHolderName = "Alic Barely";
    private CardExpiryDate expiryDate = CardExpiryDate.valueOf("12/99");
    private String cardBrand = "visa";
    private CardType cardType = CardType.DEBIT;
    private AddressEntity billingAddress = new AddressEntity(AddressFixture.anAddress().build());
    
    public static CardDetailsEntityFixture aCardDetailsEntityFixture() {
        return new CardDetailsEntityFixture();
    }
    
    public CardDetailsEntity build() {
        return new CardDetailsEntity(
            firstDigitsCardNumber,
            lastDigitsCardNumber,
            cardHolderName,
            expiryDate,
            cardBrand,
            cardType,
            billingAddress
        );
    }

    public CardDetailsEntityFixture withFirstDigitsCardNumber(FirstDigitsCardNumber firstDigitsCardNumber) {
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        return this;
    }

    public CardDetailsEntityFixture withLastDigitsCardNumber(LastDigitsCardNumber lastDigitsCardNumber) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        return this;
    }

    public CardDetailsEntityFixture withCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    public CardDetailsEntityFixture withExpiryDate(CardExpiryDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public CardDetailsEntityFixture withCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
        return this;
    }

    public CardDetailsEntityFixture withCardType(CardType cardType) {
        this.cardType = cardType;
        return this;
    }

    public CardDetailsEntityFixture withBillingAddress(AddressEntity billingAddress) {
        this.billingAddress = billingAddress;
        return this;
    }


}
