package uk.gov.hmcts.reform.pcqloader;


import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class PcqLoaderApplicationTest {

    @InjectMocks
    private PcqLoaderApplication testPcqLoaderApplication;

    @Mock
    private TelemetryClient client;

    @Mock
    private PcqLoaderComponent pcqLoaderComponent;

    @Test
    void testApplicationExecuted() throws Exception {
        testPcqLoaderApplication.run(null);
        verify(pcqLoaderComponent, times(1)).execute();
        verify(client, times(1)).flush();
    }

    @Test
    void testApplicationError() {
        assertThrows(Exception.class, () -> {
            doThrow(new Exception()).when(pcqLoaderComponent).execute();
            testPcqLoaderApplication.run(null);
        });
    }

    @Test
    void shouldCatchExceptionAndLogError(CapturedOutput output) throws Exception {

        // given
        doThrow(new IllegalArgumentException("Exception from PCQ Disposer service"))
            .when(pcqLoaderComponent).execute();

        // when
        testPcqLoaderApplication.run(null);

        // then
        verify(pcqLoaderComponent, times(1)).execute();
        assertThat(output).contains("Error executing Pcq Loader");

    }

}
