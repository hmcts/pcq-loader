package uk.gov.hmcts.reform.pcqloader.services;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;
import org.testcontainers.containers.ComposeContainer;
import uk.gov.hmcts.reform.pcqloader.config.BlobStorageProperties;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.io.FileNotFoundException;

@Slf4j
@TestPropertySource(locations = "/application.properties")
public class BlobStorageManagerBase {
    protected static final String CONTAINER_NAME = "pcq";
    protected static final String CONTAINER_REJECTED_NAME = "pcq-rejected";
    private static final String BLOB_DOWNLOAD_FILE_PATH = "/var/tmp/pcq-loader/download/blobs";
    private static final long BLOB_COPY_TIMEOUT_IN_MILLIS = 30_000;
    private static final String PROCESSED_FOLDER = "processed";
    protected static final String BLOB_FILENAME_1 = "1579002492_31-08-2020-11-35-10.zip";
    protected static final String BLOB_FILENAME_2 = "1579002493_31-08-2020-11-48-42.zip";

    protected ZipFileUtils zipFileUtils;
    protected BlobContainerClient testContainer;

    protected BlobServiceClient blobServiceClient;

    private static ComposeContainer dockerComposeContainer;

    public BlobStorageManager blobStorageManager;

    @BeforeEach
    public void setUp() throws FileNotFoundException {
        zipFileUtils = new ZipFileUtils();
        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString("UseDevelopmentStorage=true")
            .buildClient();

        testContainer = blobServiceClient.createBlobContainer(CONTAINER_NAME);
        BlobStorageProperties blobStorageProperties = new BlobStorageProperties();
        blobStorageProperties.setBlobPcqContainer(CONTAINER_NAME);
        blobStorageProperties.setBlobStorageDownloadPath(BLOB_DOWNLOAD_FILE_PATH);
        blobStorageProperties.setBlobPcqRejectedContainer(CONTAINER_REJECTED_NAME);
        blobStorageProperties.setProcessedFolderName(PROCESSED_FOLDER);
        blobStorageProperties.setBlobCopyTimeoutInMillis(BLOB_COPY_TIMEOUT_IN_MILLIS);

        File blobFile1 = ResourceUtils.getFile("classpath:blobTestFiles/" + BLOB_FILENAME_1);
        File blobFile2 = ResourceUtils.getFile("classpath:blobTestFiles/" + BLOB_FILENAME_2);

        blobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        blobStorageManager.uploadFileToBlobStorage(testContainer, blobFile1.getPath());
        blobStorageManager.uploadFileToBlobStorage(testContainer, blobFile2.getPath());
    }

    @AfterEach
    public void cleanUp() {
        testContainer.delete();
    }

    @BeforeAll
    public static void initialize() {
        log.info("Starting Azure-Storage container");
        dockerComposeContainer =
            new ComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azurite", 10_000);

        dockerComposeContainer.start();
    }

    @AfterAll
    public static void tearDownContainer() {
        log.info("Stopping Azure-Storage container");
        dockerComposeContainer.stop();
    }
}
