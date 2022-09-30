package uk.gov.hmcts.reform.pcqloader.postdeploy;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.pcqloader.PcqLoaderComponent;
import uk.gov.hmcts.reform.pcqloader.config.TestApplicationConfiguration;
import uk.gov.hmcts.reform.pcqloader.services.BlobStorageManager;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@ActiveProfiles("functional")
@Slf4j
public class PcqLoaderFunctionalTest extends PcqLoaderTestBase {

    private static final String CLASSPATH_BLOBTESTFILES_PATH = "classpath:BlobTestFiles/";
    private static final String FUNC_TEST_PCQ_CONTAINER_NAME = "pcq-func-tests";
    private static final String FUNC_TEST_PCQ_REJECTED_CONTAINER_NAME = "pcq-func-test-rejected";
    private static final String BLOB_FILENAME_1 = "1579002492_31-08-2020-11-35-10.zip";
    private static final String BLOB_FILENAME_2 = "1579002493_31-08-2020-11-48-42.zip";
    private static final String BLOB_FILENAME_3_LARGE_FILE = "1579002494_27-03-2021-12-30-00.zip";
    private static final String BLOB_FILENAME_3_INVALID_DATA = "1579002495_20-05-2021-16-40-00.zip";
    private static final String BLOB_FILENAME_3_MISSING_OCR_DATA = "1579002496_01-07-2021-16-45-00.zip";
    private static final String BLOB_CONTAINER_DEFAULT_DIR = "/";
    private static final String BLOB_CONTAINER_PROCESSED_DIR = "processed/";
    private static final int EXPECT_SUCCESSFUL_TESTS = 3;
    private static final int EXPECT_REJECTED_TESTS = 2;
    private static final int EXPECT_UNPROCESSED_TESTS = 0;

    @Autowired
    private PcqLoaderComponent pcqLoaderComponent;

    @Autowired
    private BlobStorageManager blobStorageManager;

    @Value("${functional-test.wait.period:10000}")
    private int waitPeriod;

    @Before
    public void beforeTests() throws FileNotFoundException {
        log.info("Starting PcqLoaderComponent functional tests");

        // Create test containers
        BlobContainerClient blobContainerClient = blobStorageManager.createContainer(FUNC_TEST_PCQ_CONTAINER_NAME);
        blobStorageManager.createContainer(FUNC_TEST_PCQ_REJECTED_CONTAINER_NAME);

        log.info("Created test container: {}", blobContainerClient.getBlobContainerUrl());

        // Upload sample documents
        File blobFile1 = ResourceUtils.getFile(CLASSPATH_BLOBTESTFILES_PATH + BLOB_FILENAME_1);
        File blobFile2 = ResourceUtils.getFile(CLASSPATH_BLOBTESTFILES_PATH + BLOB_FILENAME_2);
        File blobFile3 = ResourceUtils.getFile(CLASSPATH_BLOBTESTFILES_PATH + BLOB_FILENAME_3_LARGE_FILE);
        File blobFile4 = ResourceUtils.getFile(CLASSPATH_BLOBTESTFILES_PATH + BLOB_FILENAME_3_INVALID_DATA);
        File blobFile5 = ResourceUtils.getFile(CLASSPATH_BLOBTESTFILES_PATH + BLOB_FILENAME_3_MISSING_OCR_DATA);

        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile1.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile2.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile3.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile4.getPath());
        blobStorageManager.uploadFileToBlobStorage(blobContainerClient, blobFile5.getPath());
    }

    @After
    public void afterTests() throws InterruptedException {
        log.info("Stopping PcqLoaderComponent functional tests");
        log.info("Waiting for 10 Sec before deleting to allow tests to finish");
        waitToDeleteContainer();
        blobStorageManager.deleteContainer(FUNC_TEST_PCQ_CONTAINER_NAME);
        blobStorageManager.deleteContainer(FUNC_TEST_PCQ_REJECTED_CONTAINER_NAME);
    }

    @Test
    public void testExecuteMethod() {
        //Invoke the executor
        pcqLoaderComponent.execute();

        //Collect blobs
        PagedIterable<BlobItem> totalBlobs =
            blobStorageManager.getPcqContainer().listBlobs();
        assertEquals("Successful number of total blobs", EXPECT_SUCCESSFUL_TESTS, countBlobs(totalBlobs));

        PagedIterable<BlobItem> unprocessedBlobs =
            blobStorageManager.getPcqContainer().listBlobsByHierarchy(BLOB_CONTAINER_DEFAULT_DIR);
        assertEquals("No blobs should remain", EXPECT_UNPROCESSED_TESTS, countBlobs(unprocessedBlobs));

        PagedIterable<BlobItem> processedBlobs =
            blobStorageManager.getPcqContainer().listBlobsByHierarchy(BLOB_CONTAINER_PROCESSED_DIR);
        assertEquals("Successful number of processed blobs",
                     EXPECT_SUCCESSFUL_TESTS, countBlobs(processedBlobs));

        PagedIterable<BlobItem> rejectedBlobs =
            blobStorageManager.getRejectedPcqContainer().listBlobs();
        assertEquals("Number of blobs expected to be rejected",
                     EXPECT_REJECTED_TESTS, countBlobs(rejectedBlobs));
    }

    private void waitToDeleteContainer() throws InterruptedException {
        Thread.sleep(waitPeriod);
    }
}
