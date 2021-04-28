package uk.gov.hmcts.reform.pcqloader.postdeploy;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.pcqloader.PcqLoaderComponent;
import uk.gov.hmcts.reform.pcqloader.config.TestApplicationConfiguration;
import uk.gov.hmcts.reform.pcqloader.services.BlobStorageManager;

import java.io.File;
import java.io.FileNotFoundException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@ActiveProfiles("functional")
@Slf4j
public class PcqLoaderFunctionalTest extends PcqLoaderTestBase {

    private static final String FUNC_TEST_PCQ_CONTAINER_NAME = "pcq-func-tests";
    private static final String BLOB_FILENAME_1 = "1579002492_31-08-2020-11-35-10.zip";
    private static final String BLOB_FILENAME_2 = "1579002493_31-08-2020-11-48-42.zip";
    private static final String BLOB_FILENAME_3_LARGE_FILE = "1579002494_27-03-2021-12-30-00.zip";
    private static final int EXPECT_SUCCESSFUL_TESTS = 3;

    @Autowired
    private PcqLoaderComponent pcqLoaderComponent;

    @Autowired
    private BlobStorageManager blobStorageManager;


    @Before
    public void beforeTests() throws FileNotFoundException {
        log.info("Starting PcqLoaderComponent functional tests");

        // Create test container
        BlobContainerClient blobContainerClient = blobStorageManager.createContainer(FUNC_TEST_PCQ_CONTAINER_NAME);
        log.info("Created test container: {}", blobContainerClient.getBlobContainerUrl());

        // Upload sample documents
        File blobFile1 = ResourceUtils.getFile("classpath:BlobTestFiles/" + BLOB_FILENAME_1);
        File blobFile2 = ResourceUtils.getFile("classpath:BlobTestFiles/" + BLOB_FILENAME_2);
        File blobFile3 = ResourceUtils.getFile("classpath:BlobTestFiles/" + BLOB_FILENAME_3_LARGE_FILE);

        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile1.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile2.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile3.getPath());
    }

    @After
    public void afterTests() {
        log.info("Stopping PcqLoaderComponent functional tests");
        blobStorageManager.deleteContainer(FUNC_TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    public void testExecuteMethod() {
        //Invoke the executor
        pcqLoaderComponent.execute();

        //Collect blobs
        PagedIterable<BlobItem> totalBlobs =
            blobStorageManager.getPcqContainer().listBlobs();
        PagedIterable<BlobItem> unprocessedBlobs =
            blobStorageManager.getPcqContainer().listBlobsByHierarchy("/");
        PagedIterable<BlobItem> processedBlobs =
            blobStorageManager.getPcqContainer().listBlobsByHierarchy("processed/");

        //Check results
        Assertions.assertEquals(EXPECT_SUCCESSFUL_TESTS, countBlobs(totalBlobs),
                                "Successful number of total blobs");
        Assertions.assertEquals(EXPECT_SUCCESSFUL_TESTS, countBlobs(processedBlobs),
                                "Successful number of processed blobs");
        Assertions.assertEquals(0, countBlobs(unprocessedBlobs),
                                "No blobs should remain");
    }
}
