package uk.gov.hmcts.reform.pcqloader;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class PcqLoaderApplication implements ApplicationRunner {

    @Autowired
    private TelemetryClient client;

    @Autowired
    private PcqLoaderComponent pcqLoaderComponent;

    @Value("${telemetry.wait.period:10000}")
    private int waitPeriod;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        try {
            log.info("Starting the Pcq Loader job.");
            pcqLoaderComponent.execute();
            log.info("Completed the Pcq Loader job successfully.");
        } catch (Exception e) {
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
