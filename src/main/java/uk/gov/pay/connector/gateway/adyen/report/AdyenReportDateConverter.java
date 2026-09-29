package uk.gov.pay.connector.gateway.adyen.report;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class AdyenReportDateConverter {

    private AdyenReportDateConverter() {
        /* This utility class should not be instantiated */
    }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Adyen's reports zone is always in CET and are not configurable
     */
    private static final ZoneId REPORT_ZONE = ZoneId.of("CET");

    public static Instant convertToInstant(String dateValue) {
        if (isBlank(dateValue)) {
            return null;
        }

        LocalDateTime localDateTime = LocalDateTime.parse(dateValue, DATE_FORMAT);
        return localDateTime.atZone(REPORT_ZONE).toInstant();
    }
}
