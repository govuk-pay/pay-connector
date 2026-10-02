package uk.gov.pay.connector.gateway.adyen.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.connector.gateway.adyen.report.AdyenReportDateConverter.convertToInstant;

public class AdyenPayoutReportParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenPayoutReportParser.class);

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .get();

    private static final String HEADER_BALANCE_ACCOUNT = "BalanceAccount";
    private static final String HEADER_TRANSFER_ID = "Transfer Id";
    private static final String HEADER_TRANSACTION_ID = "Transaction Id";
    private static final String HEADER_CATEGORY = "Category";
    private static final String HEADER_TYPE = "Type";
    private static final String HEADER_STATUS = "Status";
    private static final String HEADER_BALANCE = "Balance (PC)";
    private static final String HEADER_REFERENCE = "Reference";
    private static final String HEADER_DESCRIPTION = "Description";
    private static final String HEADER_PSP_PAYMENT_MERCHANT_REFERENCE = "Psp Payment Merchant Reference";
    private static final String HEADER_PSP_MODIFICATION_MERCHANT_REFERENCE = "Psp Modification Merchant Reference";
    private static final String HEADER_PAYOUT = "Payout Date";

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            HEADER_BALANCE_ACCOUNT, HEADER_TRANSFER_ID, HEADER_TRANSACTION_ID,
            HEADER_CATEGORY, HEADER_TYPE, HEADER_STATUS,
            HEADER_BALANCE, HEADER_REFERENCE, HEADER_DESCRIPTION,
            HEADER_PSP_PAYMENT_MERCHANT_REFERENCE,
            HEADER_PSP_MODIFICATION_MERCHANT_REFERENCE, HEADER_PAYOUT
    );

    public List<AdyenBalancePayoutReportRecord> parse(String csvBody) {
        List<AdyenBalancePayoutReportRecord> records = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(new StringReader(csvBody), FORMAT)) {
            Set<String> headerNames = new HashSet<>(parser.getHeaderNames());
            logHeaderMismatches(headerNames);

            for (CSVRecord row : parser) {
                records.add(toRecord(row, headerNames));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Adyen payout report CSV", e);
        }

        LOGGER.atInfo()
                .setMessage("Completed parsing Adyen balance platform payout report")
                .log();
        return records;
    }

    private void logHeaderMismatches(Set<String> headersInCsv) {
        Set<String> requiredColumns = new HashSet<>(REQUIRED_COLUMNS);
        requiredColumns.removeAll(headersInCsv);
        if (!requiredColumns.isEmpty()) {
            LOGGER.warn("Adyen payout report is missing expected columns, values will be null: {}", requiredColumns);
        }
    }

    private AdyenBalancePayoutReportRecord toRecord(CSVRecord row, Set<String> headerNames) {
        String balanceAccount = safeGet(row, headerNames, HEADER_BALANCE_ACCOUNT);
        String transferId = safeGet(row, headerNames, HEADER_TRANSFER_ID);
        String transactionId = safeGet(row, headerNames, HEADER_TRANSACTION_ID);
        String category = safeGet(row, headerNames, HEADER_CATEGORY);
        String type = safeGet(row, headerNames, HEADER_TYPE);
        String status = safeGet(row, headerNames, HEADER_STATUS);
        BigDecimal balance = parseAmount(safeGet(row, headerNames, HEADER_BALANCE));
        String reference = safeGet(row, headerNames, HEADER_REFERENCE);
        String description = safeGet(row, headerNames, HEADER_DESCRIPTION);
        String pspPaymentMerchantReference = safeGet(row, headerNames, HEADER_PSP_PAYMENT_MERCHANT_REFERENCE);
        String pspModificationMerchantReference = safeGet(row, headerNames, HEADER_PSP_MODIFICATION_MERCHANT_REFERENCE);
        Instant payoutDate = convertToInstant(safeGet(row, headerNames, HEADER_PAYOUT));

        return new AdyenBalancePayoutReportRecord(balanceAccount,
                transferId,
                transactionId,
                category,
                type,
                status,
                balance,
                reference,
                description,
                pspPaymentMerchantReference,
                pspModificationMerchantReference,
                payoutDate);
    }

    private String safeGet(CSVRecord row, Set<String> headerNames, String column) {
        if (!headerNames.contains(column)) {
            return null;
        }
        String value = row.get(column);
        return isBlank(value) ? null : value;
    }

    private BigDecimal parseAmount(String value) {
        if (isBlank(value)) {
            return null;
        }
        return new BigDecimal(value);
    }
}
