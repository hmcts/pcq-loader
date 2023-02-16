package uk.gov.hmcts.reform.pcqloader.services;

import com.azure.storage.blob.BlobContainerClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcqloader.exceptions.BlobProcessingException;

import java.io.File;
import java.util.List;

@Slf4j
@TestPropertySource(locations = "/application.properties")
class BlobStorageManagerTest extends BlobStorageManagerBase {

    private static final String BLOB_FILENAME_DOES_NOT_EXIST = "NOT_FOOUND.zip";

    @Test
    void testFetchPcqStorageContainerSuccess() {
        BlobContainerClient container = blobStorageManager.getPcqContainer();
        Assertions.assertNotNull(container);
    }

    @Test
    void testCollectBlobFileNames() {
        List<String> response =
            blobStorageManager.collectBlobFileNamesFromContainer(blobStorageManager.getPcqContainer());
        Assertions.assertEquals(2, response.size(), "Correct number of blob names");
        Assertions.assertTrue(response.contains(BLOB_FILENAME_1), "Correct filename 1");
        Assertions.assertTrue(response.contains(BLOB_FILENAME_2), "Correct filename 2");
    }

    @Test
    void testCollectBlobFileNamesAfterMove() {
        blobStorageManager.moveFileToProcessedFolder(BLOB_FILENAME_1, blobStorageManager.getPcqContainer());
        List<String> response =
            blobStorageManager.collectBlobFileNamesFromContainer(blobStorageManager.getPcqContainer());
        Assertions.assertEquals(1, response.size(), "Correct number of blob names");
        Assertions.assertTrue(response.contains(BLOB_FILENAME_2), "Correct filename 2");
    }

    @Test
    void testDownloadFileFromBlobStorage() {
        File fileResponse =
            blobStorageManager.downloadFileFromBlobStorage(blobStorageManager.getPcqContainer(), BLOB_FILENAME_1);
        Assertions.assertNotNull(fileResponse,"File returned is not null");
        Assertions.assertTrue(fileResponse.getPath().contains(BLOB_FILENAME_1), "File has correct path");
    }

    @Test
    void testDownloadFileFromBlobStorageNotFoundError() {
        try {
            blobStorageManager
                .downloadFileFromBlobStorage(blobStorageManager.getPcqContainer(), BLOB_FILENAME_DOES_NOT_EXIST);
            Assertions.fail("Should generate BlobProcessingException for missing file");
        } catch (BlobProcessingException bpe) {
            Assertions.assertNotNull(bpe, "Successfully generated exception for missing file.");
        }
    }

    @Test
    void testMoveFileToRejectedContainer() {
        blobStorageManager.moveFileToRejectedContainer(BLOB_FILENAME_1, blobStorageManager.getPcqContainer());
        File rejectedFile = blobStorageManager.downloadFileFromBlobStorage(
            blobStorageManager.getRejectedPcqContainer(), BLOB_FILENAME_1);
        Assertions.assertNotNull(rejectedFile, "Rejected file does not exist");
        try {
            blobStorageManager.downloadFileFromBlobStorage(
                blobStorageManager.getPcqContainer(), BLOB_FILENAME_1);
        } catch (BlobProcessingException bpe) {
            Assertions.assertNotNull(bpe, "Successfully generated exception for missing file");
        }
    }

    @Test
    void testMoveFileToProcessedFolder() {
        blobStorageManager.moveFileToProcessedFolder(BLOB_FILENAME_1, blobStorageManager.getPcqContainer());
        try {
            blobStorageManager.downloadFileFromBlobStorage(
                blobStorageManager.getPcqContainer(), BLOB_FILENAME_1);
        } catch (BlobProcessingException bpe) {
            Assertions.assertNotNull(bpe, "Successfully generated exception for missing file");
        }

        File processedFile = blobStorageManager.downloadFileFromBlobStorage(
                blobStorageManager.getPcqContainer(), "processed" + File.separator + BLOB_FILENAME_1);
        Assertions.assertNotNull(processedFile, "Processed file does not exist");

    }
}
