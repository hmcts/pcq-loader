package uk.gov.hmcts.reform.pcqloader;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;

import static uk.gov.hmcts.reform.pcqloader.utils.PcqLoaderConstants.ERROR_TYPE;
import static uk.gov.hmcts.reform.pcqloader.utils.PcqLoaderConstants.PCQ_LOADER_ERROR_MARKER;


@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.reform")
@EnableConfigurationProperties
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.pcq.commons"})
@Slf4j
@RequiredArgsConstructor
public class PcqLoaderApplication implements ApplicationRunner {

    private final TelemetryClient client;

    private final PcqLoaderComponent pcqLoaderComponent;

    @Value("${telemetry.wait.period:10000}")
    private int waitPeriod;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        try {
            log.info("Starting the Pcq Loader job.");
            pcqLoaderComponent.execute();
            log.info("Completed the Pcq Loader job successfully.");
        } catch (Exception e) {
            MDC.put(ERROR_TYPE, PCQ_LOADER_ERROR_MARKER);
            log.error("Error executing Pcq Loader", e);
        } finally {
            MDC.clear();
            client.flush();
            waitTelemetryGracefulPeriod();
        }

    }

    private void waitTelemetryGracefulPeriod() throws InterruptedException {
        Thread.sleep(waitPeriod);
    }

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(PcqLoaderApplication.class);
        SpringApplication.exit(context);
    }
}
