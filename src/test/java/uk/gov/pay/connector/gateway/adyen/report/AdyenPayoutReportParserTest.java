package uk.gov.pay.connector.gateway.adyen.report;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_BALANCE_PLAYFORM_REPORT;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

class AdyenPayoutReportParserTest {

    private AdyenPayoutReportParser parser;

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(AdyenPayoutReportParser.class);

    @BeforeEach
    void setUp() {
        parser = new AdyenPayoutReportParser();
    }

    @Test
    void shouldParseCsvRowsWithDifferentRecordsCorrectly() {
        String payoutReportCsv = load(ADYEN_BALANCE_PLAYFORM_REPORT);

        List<AdyenBalancePayoutReportRecord> records = parser.parse(payoutReportCsv);

        assertThat(records, hasSize(3));
        AdyenBalancePayoutReportRecord captureReportRecord = records.get(0);
        assertPayoutReportRecord(captureReportRecord, "capture", "captured",
                "charge-external-id-123", "2.58",
                "3JY1Y65VXYY7GD3F",
                "platformPayment", null);
        AdyenBalancePayoutReportRecord refundReportRecord = records.get(1);
        assertPayoutReportRecord(refundReportRecord, "refund", "refunded",
                null, "-0.12",
                "3JY1Y65VXWKY63O8",
                "platformPayment", "refund-external-id-123");
        AdyenBalancePayoutReportRecord payoutReportRecord = records.get(2);
        assertPayoutReportRecord(payoutReportRecord, "bankTransfer", "booked",
                null, "-2.46",
                "3CY1XOPVXWKYA3O9",
                "bank", null);

        logs.assertContains("Completed parsing Adyen balance platform payout report");
    }

    private static void assertPayoutReportRecord(AdyenBalancePayoutReportRecord payoutReportRecord,
                                                 String expectedType,
                                                 String expectedStatus,
                                                 String expectedMerchantReference,
                                                 String expectedAmount,
                                                 String expectedTransferId,
                                                 String expectedCategory,
                                                 String pspModificationMerchantReference) {
        assertThat(payoutReportRecord.balanceAccount(), is("BA00000000000000000000001"));
        assertThat(payoutReportRecord.transferId(), is(expectedTransferId));
        assertThat(payoutReportRecord.category(), is(expectedCategory));
        assertThat(payoutReportRecord.type(), is(expectedType));
        assertThat(payoutReportRecord.status(), is(expectedStatus));
        assertThat(payoutReportRecord.balance(), is(new BigDecimal(expectedAmount)));

        assertThat(payoutReportRecord.pspPaymentMerchantReference(), is(expectedMerchantReference));
        assertThat(payoutReportRecord.pspModificationMerchantReference(), is(pspModificationMerchantReference));
        assertThat(payoutReportRecord.payoutDate(), is(Instant.parse("2023-12-15T06:00:12Z")));
    }

    @Test
    void parsesFieldsWithQuotedCommasCorrectly() {
        String csv = load(ADYEN_BALANCE_PLAYFORM_REPORT)
                .replace("YOUR_DESCRIPTION_FOR_THE_TRANSFER", "\"Refund, requested by customer\"");

        List<AdyenBalancePayoutReportRecord> records = parser.parse(csv);

        assertThat(records, hasSize(3));
        assertThat(records.getFirst().description(), is("Refund, requested by customer"));
    }

    @Test
    void shouldIgnoreUnknownColumns() {
        String csv = load(ADYEN_BALANCE_PLAYFORM_REPORT)
                .replace("Psp Modification Merchant Reference", "Psp Modification Merchant Reference,Store");

        List<AdyenBalancePayoutReportRecord> records = parser.parse(csv);

        assertThat(records, hasSize(3));
        assertThat(records.getFirst().pspPaymentMerchantReference(), is("charge-external-id-123"));
    }

    @Test
    void missingKnownColumnResultsInNullFieldRatherThanThrowing() {
        String csv = load(ADYEN_BALANCE_PLAYFORM_REPORT)
                .replace("Psp Payment Merchant Reference", "some-other-header");

        List<AdyenBalancePayoutReportRecord> records = parser.parse(csv);

        assertThat(records, hasSize(3));
        assertThat(records.getFirst().pspPaymentMerchantReference(), is(nullValue()));

        logs.assertContains("Adyen payout report is missing expected columns, values will be null");
    }

    @Test
    void blankCellsAreParsedAsNullNotEmptyString() {
        String csv = load(ADYEN_BALANCE_PLAYFORM_REPORT)
                .replace("charge-external-id-123", "     ");

        List<AdyenBalancePayoutReportRecord> records = parser.parse(csv);

        assertThat(records, hasSize(3));
        assertThat(records.getFirst().pspPaymentMerchantReference(), is(nullValue()));
    }
}
