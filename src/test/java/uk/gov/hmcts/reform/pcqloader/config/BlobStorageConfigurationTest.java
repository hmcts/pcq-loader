package uk.gov.hmcts.reform.pcqloader.config;

import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlobStorageConfigurationTest {

    @InjectMocks
    private BlobStorageConfiguration blobStorageConfiguration;

    private static final String ACCOUNT_NAME = "pcqshared";
    private static final String ACCOUNT_KEY = "12345678";
    private static final String ACCOUNT_URL = "http://pcqshared.file.core.windows.net";

    @Test
    void blobStorageConfigurationGetStorageClientTest() {
        BlobServiceClient blobServiceClient = blobStorageConfiguration.getStorageClient(
            ACCOUNT_NAME, ACCOUNT_KEY, ACCOUNT_URL);
        Assertions.assertEquals(ACCOUNT_NAME, blobServiceClient.getAccountName(), "Account name should match");
        Assertions.assertEquals(ACCOUNT_URL, blobServiceClient.getAccountUrl(), "Account URL should match");
    }
}
