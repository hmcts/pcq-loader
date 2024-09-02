package uk.gov.hmcts.reform.pcqloader;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;


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
            //This specific message error has been added in Azure log to look for these traces in alert
            // query and create alert if disposer-idam-user throw any exception because of any reason.
            log.error("Error executing Pcq Loader : " +  e);
            //To have stack trace of this exception as we are catching the exception
            // stack trace will not be logged by azure
            log.error("Error executing Pcq Loader", e);
        } finally {
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
