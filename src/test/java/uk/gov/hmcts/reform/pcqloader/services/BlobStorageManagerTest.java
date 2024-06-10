package uk.gov.hmcts.reform.pcqloader.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcqloader.config.BlobStorageProperties;
import uk.gov.hmcts.reform.pcqloader.exceptions.BlobProcessingException;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.TooManyMethods")
@ExtendWith(MockitoExtension.class)
class BlobStorageManagerTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient pcqContainer;

    @Mock
    private PagedIterable<BlobItem> pageIterableBlobs;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobContainerClient rejectedPcqContainer;

    @Mock
    private BlobClient rejectedBlobClient;

    @Mock
    private BlobClient processedBlobClient;

    @Mock
    private ZipFileUtils zipFileUtilsMock;

    private BlobStorageProperties blobStorageProperties;

    private BlobStorageManager testBlobStorageManager;

    private ZipFileUtils zipFileUtils;

    private static final String TEST_PCQ_CONTAINER_NAME = "PCQ_1";
    private static final String TEST_CUSTOM_CONTAINER_NAME = "PCQ_2";
    private static final String TEST_BLOB_FILENAME1 = "1579002492_31-08-2020-11-35-10.zip";
    private static final String TEST_BLOB_FILENAME2 = "1579002493_31-08-2020-11-48-42.zip";
    private static final String TEST_PCQ_BLOB_PATH = "http://0.0.0.0:10000/" + TEST_PCQ_CONTAINER_NAME;
    private static final String TEST_PCQ_FILE_PATH = "/var/tmp/pcq-blobs";
    private static final String TEST_REJECTED_PCQ_CONTAINER_NAME = "PCQ_REJECTED";
    private static final String PROCESSED_FOLDER = "processed";
    private static final String ROOT_FOLDER = "";

    @BeforeEach
    void setUp() {
        zipFileUtils = new ZipFileUtils();
        blobStorageProperties = new BlobStorageProperties();
        blobStorageProperties.setBlobPcqContainer(TEST_PCQ_CONTAINER_NAME);
        blobStorageProperties.setBlobStorageDownloadPath(TEST_PCQ_FILE_PATH);
        blobStorageProperties.setBlobPcqRejectedContainer(TEST_REJECTED_PCQ_CONTAINER_NAME);
        blobStorageProperties.setProcessedFolderName(PROCESSED_FOLDER);
        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
    }

    @Test
    void initialisationSuccess() {
        blobServiceClient.getBlobContainerClient(TEST_PCQ_CONTAINER_NAME);
        verify(blobServiceClient, times(1)).getBlobContainerClient(TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    void testGetPcqContainer() {
        when(blobServiceClient.getBlobContainerClient(TEST_PCQ_CONTAINER_NAME)).thenReturn(pcqContainer);
        BlobContainerClient blobContainerClient = testBlobStorageManager.getPcqContainer();
        Assertions.assertNotNull(blobContainerClient, "Container collected successfully");
        verify(blobServiceClient, times(1)).getBlobContainerClient(TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    void testGetContainerWithContainerName() {
        when(blobServiceClient.getBlobContainerClient(TEST_CUSTOM_CONTAINER_NAME)).thenReturn(pcqContainer);
        BlobContainerClient blobContainerClient = testBlobStorageManager.getContainer(TEST_CUSTOM_CONTAINER_NAME);
        Assertions.assertNotNull(blobContainerClient, "Named container collected successfully");
        verify(blobServiceClient, times(1)).getBlobContainerClient(TEST_CUSTOM_CONTAINER_NAME);
    }

    @Test
    void testCreateNewContainerSuccess() {
        when(blobServiceClient.createBlobContainer(TEST_PCQ_CONTAINER_NAME)).thenReturn(pcqContainer);
        when(blobServiceClient.getBlobContainerClient(TEST_PCQ_CONTAINER_NAME)).thenReturn(pcqContainer);
        when(pcqContainer.exists()).thenReturn(false);
        BlobContainerClient newContainer = testBlobStorageManager.createContainer(TEST_PCQ_CONTAINER_NAME);
        Assertions.assertNotNull(newContainer, "Container created");
        verify(blobServiceClient, times(1)).createBlobContainer(TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    void testCreateNewContainerWhenExistingSuccess() {
        when(blobServiceClient.getBlobContainerClient(TEST_PCQ_CONTAINER_NAME)).thenReturn(pcqContainer);
        when(pcqContainer.exists()).thenReturn(true);
        BlobContainerClient newContainer = testBlobStorageManager.createContainer(TEST_PCQ_CONTAINER_NAME);
        Assertions.assertNotNull(newContainer, "Container created");
        verify(blobServiceClient, times(2)).getBlobContainerClient(TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    void testUploadFilesToBlobStorageSuccess() {
        when(pcqContainer.getBlobClient(TEST_BLOB_FILENAME1)).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(TEST_PCQ_BLOB_PATH + "/" + TEST_BLOB_FILENAME1);
        testBlobStorageManager.uploadFileToBlobStorage(pcqContainer, TEST_BLOB_FILENAME1);
        verify(blobClient, times(1)).uploadFromFile(TEST_BLOB_FILENAME1);
    }

    @Test
    void testDeleteContainersSuccess() {
        testBlobStorageManager.deleteContainer(TEST_PCQ_CONTAINER_NAME);
        verify(blobServiceClient, times(1)).deleteBlobContainer(TEST_PCQ_CONTAINER_NAME);
    }

    @Test
    void testCollectBlobFileNamesSuccess() {
        BlobItem blobItem1 = new BlobItem().setName(TEST_BLOB_FILENAME1).setDeleted(false).setIsPrefix(false);
        BlobItem blobItem2 = new BlobItem().setName(TEST_BLOB_FILENAME2).setDeleted(false).setIsPrefix(false);
        List<BlobItem> blobs = Arrays.asList(blobItem1, blobItem2);

        when(pcqContainer.listBlobsByHierarchy(ROOT_FOLDER)).thenReturn(pageIterableBlobs);
        when(pageIterableBlobs.iterator()).thenReturn(blobs.iterator());

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        List<String> response = testBlobStorageManager.collectBlobFileNamesFromContainer(pcqContainer);

        verify(pageIterableBlobs, times(1)).iterator();
        Assertions.assertEquals(2, response.size(), "Correct number of blob names");
        Assertions.assertTrue(response.contains(TEST_BLOB_FILENAME1), "Correct filename 1");
        Assertions.assertTrue(response.contains(TEST_BLOB_FILENAME2), "Correct filename 2");
    }

    @Test
    void testCollectBlobFileNamesEmptyListSuccess() {
        List<BlobItem> blobs = new ArrayList<>();

        when(pcqContainer.listBlobsByHierarchy(ROOT_FOLDER)).thenReturn(pageIterableBlobs);
        when(pageIterableBlobs.iterator()).thenReturn(blobs.iterator());

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        List<String> response = testBlobStorageManager.collectBlobFileNamesFromContainer(pcqContainer);

        verify(pageIterableBlobs, times(1)).iterator();
        Assertions.assertEquals(0, response.size(), "Correct number of blob names");
    }

    @Test
    void testCollectBlobFileNamesWithNameNull() {
        BlobItem blobItem1 = new BlobItem().setDeleted(false).setIsPrefix(false).setName(null);
        var blobs = Arrays.asList(blobItem1);

        when(pcqContainer.listBlobsByHierarchy(ROOT_FOLDER)).thenReturn(pageIterableBlobs);
        when(pageIterableBlobs.iterator()).thenReturn(blobs.iterator());

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        List<String> response = testBlobStorageManager.collectBlobFileNamesFromContainer(pcqContainer);

        verify(pageIterableBlobs, times(1)).iterator();
        Assertions.assertEquals(0, response.size(),
                                "Response size should be 0 as no files should be added as name provided was null.");
    }

    @Test
    void testCollectBlobFileNamesWithPrefixTrue() {
        BlobItem blobItem1 = new BlobItem().setIsPrefix(true);
        var blobs = Arrays.asList(blobItem1);

        when(pcqContainer.listBlobsByHierarchy(ROOT_FOLDER)).thenReturn(pageIterableBlobs);
        when(pageIterableBlobs.iterator()).thenReturn(blobs.iterator());

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        List<String> response = testBlobStorageManager.collectBlobFileNamesFromContainer(pcqContainer);

        verify(pageIterableBlobs, times(1)).iterator();
        Assertions.assertEquals(0, response.size(),
                                "Response size should be 0 as no files should be added to list as isPrefix is true");
    }

    @Test
    void testDownloadFileFromBlobStorageSuccess() throws IOException {

        when(pcqContainer.getBlobClient(TEST_BLOB_FILENAME1)).thenReturn(blobClient);
        when(blobClient.downloadToFile(TEST_PCQ_FILE_PATH + File.separator + TEST_BLOB_FILENAME1,
                                       true)).thenReturn(null);
        File downloadFile = new File(TEST_PCQ_FILE_PATH + File.separator + TEST_BLOB_FILENAME1);
        downloadFile.createNewFile();

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);
        File fileResponse = testBlobStorageManager.downloadFileFromBlobStorage(pcqContainer, TEST_BLOB_FILENAME1);

        verify(blobClient, times(1)).downloadToFile(
            TEST_PCQ_FILE_PATH + "/" + TEST_BLOB_FILENAME1, true);
        Assertions.assertNotNull(fileResponse, "File response is not null");
    }

    @Test
    void testDownloadFileFromBlobStorageError() {
        when(pcqContainer.getBlobClient(TEST_BLOB_FILENAME1)).thenReturn(blobClient);
        when(blobClient.downloadToFile(TEST_PCQ_FILE_PATH + File.separator + TEST_BLOB_FILENAME1,
                                       true)).thenReturn(null);
        File downloadFile = new File(TEST_PCQ_FILE_PATH + File.separator + TEST_BLOB_FILENAME1);
        downloadFile.delete();

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtils);

        try {
            testBlobStorageManager.downloadFileFromBlobStorage(pcqContainer, TEST_BLOB_FILENAME1);
            Assertions.fail("BlobProcessingException should be thrown");
        } catch (BlobProcessingException bpe) {
            verify(blobClient, times(1)).downloadToFile(
                TEST_PCQ_FILE_PATH + "/" + TEST_BLOB_FILENAME1, true);
        }
    }

    @Test
    void testDownloadFileFromBlobStorageWriteFileError() {
        when(zipFileUtilsMock.confirmFileCanBeCreated(ArgumentMatchers.any())).thenReturn(Boolean.FALSE);

        testBlobStorageManager = new BlobStorageManager(blobStorageProperties, blobServiceClient, zipFileUtilsMock);

        try {
            testBlobStorageManager.downloadFileFromBlobStorage(pcqContainer, TEST_BLOB_FILENAME1);
            Assertions.fail("BlobProcessingException should be thrown");
        } catch (BlobProcessingException bpe) {
            verify(blobClient, times(0)).downloadToFile(
                TEST_PCQ_FILE_PATH + "/" + TEST_BLOB_FILENAME1, true);
        }
    }

    @Test
    void testMoveFileToRejectedContainer1() {
        String testFileName = "Test1212.zip";
        when(blobServiceClient.getBlobContainerClient(TEST_REJECTED_PCQ_CONTAINER_NAME))
            .thenReturn(rejectedPcqContainer);
        when(rejectedPcqContainer.exists()).thenReturn(false);
        when(blobServiceClient.createBlobContainer(TEST_REJECTED_PCQ_CONTAINER_NAME)).thenReturn(rejectedPcqContainer);
        when(pcqContainer.getBlobClient(testFileName)).thenReturn(blobClient);
        when(rejectedPcqContainer.getBlobClient(testFileName)).thenReturn(rejectedBlobClient);
        when(blobClient.getBlobUrl()).thenReturn(TEST_PCQ_BLOB_PATH + "/" + testFileName);
        when(rejectedBlobClient.beginCopy(TEST_PCQ_BLOB_PATH + "/" + testFileName, null))
            .thenReturn(null);
        doNothing().when(blobClient).delete();

        testBlobStorageManager.moveFileToRejectedContainer(testFileName, pcqContainer);

        verify(blobServiceClient, times(1)).getBlobContainerClient(TEST_REJECTED_PCQ_CONTAINER_NAME);
        verify(rejectedPcqContainer, times(1)).exists();
        verify(blobServiceClient, times(1)).createBlobContainer(TEST_REJECTED_PCQ_CONTAINER_NAME);
        verify(pcqContainer, times(1)).getBlobClient(testFileName);
        verify(rejectedPcqContainer, times(1)).getBlobClient(testFileName);
        verify(blobClient, times(1)).getBlobUrl();
        verify(rejectedBlobClient, times(1)).beginCopy(TEST_PCQ_BLOB_PATH + "/"
                                                             + testFileName, null);
        verify(blobClient, times(1)).delete();
    }

    @Test
    void testMoveFileToRejectedContainer2() {
        String testFileName = "Test1213.zip";
        when(blobServiceClient.getBlobContainerClient(TEST_REJECTED_PCQ_CONTAINER_NAME))
            .thenReturn(rejectedPcqContainer);
        when(rejectedPcqContainer.exists()).thenReturn(true);
        when(pcqContainer.getBlobClient(testFileName)).thenReturn(blobClient);
        when(rejectedPcqContainer.getBlobClient(testFileName)).thenReturn(rejectedBlobClient);
        when(blobClient.getBlobUrl()).thenReturn(TEST_PCQ_BLOB_PATH + "/" + testFileName);
        when(rejectedBlobClient.beginCopy(TEST_PCQ_BLOB_PATH + "/" + testFileName, null))
            .thenReturn(null);
        doNothing().when(blobClient).delete();

        testBlobStorageManager.moveFileToRejectedContainer(testFileName, pcqContainer);

        verify(blobServiceClient, times(2)).getBlobContainerClient(TEST_REJECTED_PCQ_CONTAINER_NAME);
        verify(rejectedPcqContainer, times(1)).exists();
        verify(pcqContainer, times(1)).getBlobClient(testFileName);
        verify(rejectedPcqContainer, times(1)).getBlobClient(testFileName);
        verify(blobClient, times(1)).getBlobUrl();
        verify(rejectedBlobClient, times(1)).beginCopy(TEST_PCQ_BLOB_PATH + "/"
                                                             + testFileName, null);
        verify(blobClient, times(1)).delete();
    }

    @Test
    void testMoveFileToProcessedFolder() {
        String testFileName = "Test121334.zip";
        when(pcqContainer.getBlobClient(testFileName)).thenReturn(blobClient);
        when(pcqContainer.getBlobClient(PROCESSED_FOLDER + "/" + testFileName)).thenReturn(processedBlobClient);
        when(blobClient.getBlobUrl()).thenReturn(TEST_PCQ_BLOB_PATH + "/" + testFileName);
        when(processedBlobClient.beginCopy(TEST_PCQ_BLOB_PATH + "/" + testFileName, null))
            .thenReturn(null);
        doNothing().when(blobClient).delete();

        testBlobStorageManager.moveFileToProcessedFolder(testFileName, pcqContainer);

        verify(pcqContainer, times(1)).getBlobClient(testFileName);
        verify(pcqContainer, times(1)).getBlobClient(PROCESSED_FOLDER + "/" + testFileName);
        verify(blobClient, times(1)).getBlobUrl();
        verify(processedBlobClient, times(1)).beginCopy(TEST_PCQ_BLOB_PATH
                                                              + "/" + testFileName, null);

        verify(blobClient, times(1)).delete();
    }
}
