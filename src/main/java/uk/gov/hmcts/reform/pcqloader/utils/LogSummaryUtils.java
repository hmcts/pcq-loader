package uk.gov.hmcts.reform.pcqloader.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class LogSummaryUtils {

    private static final String SUMMARY_HEADING_STRING = "\r\nPCQ Loader Record Summary : ";
    private static final String FORMAT_STR_LENGTH_30 = "%1$-30s";
    private static final String SERVICE_SUMMARY_STRING = String.format(FORMAT_STR_LENGTH_30, "Service")
        + "Created | Errors\r\n";
    private static final String CR_STRING = "\r\n";
    private static final String TAB_STRING = "| ";
    private static final String ERROR_SUFFIX = "_Erred";
    private static final String CRATED_SUFFIX = "_Created";
    private static final String FORMAT_STR_LENGTH_8 = "%1$-8s";
    private static final String TOTAL_STRING = "Total";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH mm", Locale.UK);

    private LogSummaryUtils() {
        //No Args private constructor.
    }

    public static void logSummary(Map<String, Integer> serviceSummaryMap) {
        StringBuilder stringBuilder = new StringBuilder(getSummaryString());

        AtomicInteger totalCreated = new AtomicInteger();
        AtomicInteger totalErrors = new AtomicInteger();

        stringBuilder.append(getServiceSummaryString(serviceSummaryMap, totalCreated, totalErrors))
            .append(String.format(FORMAT_STR_LENGTH_30,TOTAL_STRING))
            .append(String.format(FORMAT_STR_LENGTH_8,totalCreated.intValue()))
            .append(TAB_STRING)
            .append(totalErrors.intValue());

        log.info(stringBuilder.toString());
    }

    private static String getSummaryString() {
        StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder
            .append(SUMMARY_HEADING_STRING)
            .append(LocalDateTime.now().format(DATE_FORMATTER))
            .append(CR_STRING)
            .append(SERVICE_SUMMARY_STRING)
            .append("-----------------------------------------------------------")
            .append(CR_STRING);
        return stringBuilder.toString();
    }

    private static String getServiceSummaryString(Map<String, Integer> serviceSummaryMap,
                                                  AtomicInteger totalCreated, AtomicInteger totalErrors) {
        Set<String> serviceKeySet = serviceSummaryMap.keySet();
        StringBuilder stringBuilder = new StringBuilder();

        serviceKeySet.forEach(service -> {
            String jurisdiction = service.substring(0, service.indexOf('_'));
            if (jurisdiction.isBlank()) {
                stringBuilder.append(String.format(FORMAT_STR_LENGTH_30,"UNKNOWN"));
            } else {
                stringBuilder.append(String.format(FORMAT_STR_LENGTH_30, jurisdiction.toUpperCase(Locale.UK)));
            }
            Integer createdCount = serviceSummaryMap.get(jurisdiction + CRATED_SUFFIX);
            Integer erredCount =  serviceSummaryMap.get(jurisdiction + ERROR_SUFFIX);
            stringBuilder.append(countsString(createdCount, erredCount));
            totalCreated.addAndGet(createdCount == null ? 0 : createdCount);
            totalErrors.addAndGet(erredCount == null ? 0 : erredCount);
        });

        return stringBuilder.toString();
    }

    private static String countsString(Integer createdCount, Integer erredCount) {

        return String.format(FORMAT_STR_LENGTH_8, createdCount == null ? 0 : createdCount)
            + TAB_STRING
            + (erredCount == null ? 0 : erredCount)
            + CR_STRING;
    }
}
