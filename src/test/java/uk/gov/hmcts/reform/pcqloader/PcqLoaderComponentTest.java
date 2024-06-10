package uk.gov.hmcts.reform.pcqloader;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcqloader.exceptions.BlobProcessingException;
import uk.gov.hmcts.reform.pcqloader.exceptions.ZipProcessingException;
import uk.gov.hmcts.reform.pcqloader.helper.PayloadMappingHelper;
import uk.gov.hmcts.reform.pcqloader.services.BlobStorageManager;
import uk.gov.hmcts.reform.pcqloader.services.PcqBackendService;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class PcqLoaderComponentTest {

    private static final String TEST_BLOB_FILENAME1 = "1579002492_31-08-2020-11-35-10.zip";
    private static final String TEST_BLOB_FILENAME2 = "1579002493_31-08-2020-11-48-42.zip";
    private static final String TEST_EXCEPTION_MSG = "Test Exception";
    private static final String SUCCESS_MSG = "Successfully created";
    private static final String HTTP_CREATED = "201";
    private static final String EXCEPTION_UNEXPECTED = "Exception not expected ";
    private static final String PAYLOAD_TEST_FILE = "testPayloadFiles/successMetaFile.json";

    @InjectMocks
    private PcqLoaderComponent pcqLoaderComponent;

    @Mock
    private BlobStorageManager blobStorageManager;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private PayloadMappingHelper payloadMappingHelper;

    @Mock
    private PcqBackendService pcqBackendService;

    @Mock
    private ZipFileUtils fileUtil;

    @Mock
    private File zipDirectory;

    @Mock
    private File unzippedFile;


    @Test
    void executeSuccess() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest)
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(2)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void testContainerNotAvailable() {

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(false);

        try {
            pcqLoaderComponent.execute();
        } catch (IllegalArgumentException iae) {
            verify(blobStorageManager, times(1)).getPcqContainer();
            verify(blobContainerClient, times(1)).exists();
        }

    }

    @Test
    void testNoZipFilesAvailable() {
        List<String> blobFileNames = new ArrayList<>();

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);

    }

    @Test
    void testBlobDownloadErrorForOneFile() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        BlobProcessingException blobProcessingException = new BlobProcessingException("Test Message");
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenThrow(blobProcessingException);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(null, null);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(1)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(null, null);

    }

    @Test
    void testZipProcessingExceptionForOneFile() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        ZipProcessingException zipProcessingException = new ZipProcessingException("Test Message");
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";
        File testZipDirectory = mock(File.class);

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(testZipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(fileUtil.unzipBlobDownloadZipFile(testZipDirectory)).thenThrow(zipProcessingException);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(testZipDirectory, null);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(1)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(testZipDirectory, null);

    }

    @Test
    void testEmptyZipFile() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        File[] emptyListedFiles = {};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";
        File testZipDirectory = mock(File.class);
        File testUnzippedFile = mock(File.class);

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(testZipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(fileUtil.unzipBlobDownloadZipFile(testZipDirectory)).thenReturn(testUnzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(testUnzippedFile.listFiles()).thenReturn(emptyListedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(testZipDirectory, testUnzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(testZipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(testUnzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(1)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(testZipDirectory, testUnzippedFile);

    }

    @Test
    void testInvalidMetaDataFileName() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File invalidMetaDataFile = new File("metadataProbate.json");
        File[] listedFiles = {metaDataFile};
        File[] invalidListedFiles = {invalidMetaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";
        File testZipDirectory = mock(File.class);
        File testUnzippedFile = mock(File.class);

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(testZipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(fileUtil.unzipBlobDownloadZipFile(testZipDirectory)).thenReturn(testUnzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(testUnzippedFile.listFiles()).thenReturn(invalidListedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);
        when(fileUtil.getMetaDataFile(invalidListedFiles)).thenReturn(null);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(testZipDirectory, testUnzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(testZipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(testUnzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        verify(fileUtil, times(1)).getMetaDataFile(invalidListedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(1)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(testZipDirectory, testUnzippedFile);

    }

    @Test
    void testNullListFile() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(null);

        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();

        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeSuccessAndMappingError() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(null)
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED, SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToRejectedContainer(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(1)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToRejectedContainer(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeMappingErrorNoSuchField() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenThrow(new NoSuchFieldException(TEST_EXCEPTION_MSG))
                .thenThrow(new NoSuchFieldException(TEST_EXCEPTION_MSG));
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeMappingErrorIllegalAccessException() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenThrow(new IllegalAccessException(TEST_EXCEPTION_MSG))
                .thenThrow(new IllegalAccessException(TEST_EXCEPTION_MSG));
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeSuccessDuplicateDcn() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest)
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          "409",
                                                                          "Invalid Request");
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(2)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeInvalidRequest() {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1, TEST_BLOB_FILENAME2);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest)
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          "400",
                                                                          "Invalid Request");
        when(pcqBackendService.submitAnswers(answerRequest)).thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToRejectedContainer(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(blobStorageManager).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME2);
        verify(fileUtil, times(2)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(2)).listFiles();
        verify(fileUtil, times(2)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(2)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(2)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(2)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToRejectedContainer(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(blobStorageManager, times(1)).moveFileToRejectedContainer(TEST_BLOB_FILENAME2, blobContainerClient);
        verify(fileUtil, times(2)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeInvalidApiException() {

        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        when(pcqBackendService.submitAnswers(answerRequest)).thenThrow(
            new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, "Test Error"));
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(3)).submitAnswers(answerRequest);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void executeInvalidApiExceptionSuccessfulThirdAttempt() {

        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);

        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }

        ResponseEntity<Map<String, String>> successResponse = getResponse(answerRequest.getPcqId(),
                                                                          HTTP_CREATED,
                                                                          SUCCESS_MSG);
        when(pcqBackendService.submitAnswers(answerRequest)).thenThrow(
            new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, "Test Error"))
            .thenThrow(new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, "Test Error 2nd Time"))
            .thenReturn(successResponse);
        doNothing().when(blobStorageManager).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        doNothing().when(fileUtil).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

        pcqLoaderComponent.execute();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
        verify(blobStorageManager, times(1)).downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1);
        verify(fileUtil, times(1)).unzipBlobDownloadZipFile(zipDirectory);
        verify(unzippedFile, times(1)).listFiles();
        verify(fileUtil, times(1)).getMetaDataFile(listedFiles);
        try {
            verify(fileUtil, times(1)).readAllBytesFromFile(metaDataFile);
            verify(payloadMappingHelper, times(1)).mapPayLoadToPcqAnswers(
                jsonTestMetaData);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        verify(pcqBackendService, times(3)).submitAnswers(answerRequest);
        verify(blobStorageManager, times(1)).moveFileToProcessedFolder(TEST_BLOB_FILENAME1, blobContainerClient);
        verify(fileUtil, times(1)).deleteFilesFromLocalStorage(zipDirectory, unzippedFile);

    }

    @Test
    void testInterruptedException() throws InterruptedException {
        List<String> blobFileNames = Arrays.asList(TEST_BLOB_FILENAME1);
        File metaDataFile = new File(PAYLOAD_TEST_FILE);
        File[] listedFiles = {metaDataFile};
        PcqAnswerRequest answerRequest = getAnswerRequest();
        String jsonTestMetaData = "{}";

        when(blobStorageManager.getPcqContainer()).thenReturn(blobContainerClient);
        when(blobContainerClient.exists()).thenReturn(true);
        when(blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient)).thenReturn(blobFileNames);
        when(blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, TEST_BLOB_FILENAME1))
            .thenReturn(zipDirectory);
        when(fileUtil.unzipBlobDownloadZipFile(zipDirectory)).thenReturn(unzippedFile);
        when(unzippedFile.listFiles()).thenReturn(listedFiles);
        when(fileUtil.getMetaDataFile(listedFiles)).thenReturn(metaDataFile);
        try {
            when(fileUtil.readAllBytesFromFile(metaDataFile)).thenReturn(jsonTestMetaData);
            when(payloadMappingHelper.mapPayLoadToPcqAnswers(jsonTestMetaData))
                .thenReturn(answerRequest);
        } catch (Exception e) {
            fail(EXCEPTION_UNEXPECTED + e.getMessage());
        }
        when(pcqBackendService.submitAnswers(answerRequest)).thenThrow(
                new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, "Test Error"));

        Thread thread = new Thread() {
            @Override
            public void run() {
                pcqLoaderComponent.execute();
            }
        };

        thread.start();
        thread.interrupt();
        assertTrue("Thread is not Interrupted", thread.isInterrupted());
        thread.join();

        verify(blobStorageManager, times(1)).getPcqContainer();
        verify(blobContainerClient, times(1)).exists();
        verify(blobStorageManager, times(1)).collectBlobFileNamesFromContainer(blobContainerClient);
    }


    private PcqAnswerRequest getAnswerRequest() {
        PcqAnswerRequest pcqAnswerRequest = new PcqAnswerRequest();
        pcqAnswerRequest.setPartyId("PaperForm");
        pcqAnswerRequest.setDcnNumber("testDcn");
        pcqAnswerRequest.setChannel(2);
        pcqAnswerRequest.setPcqId("Test_Pcq_Id");
        pcqAnswerRequest.setServiceId("Probate");
        PcqAnswers answers = new PcqAnswers();
        answers.setDobProvided(0);
        pcqAnswerRequest.setPcqAnswers(answers);

        return pcqAnswerRequest;
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private ResponseEntity<Map<String, String>> getResponse(String pcqId, String statusCode, String statusMessage) {
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("pcqid", pcqId);
        responseMap.put("responseStatusCode", statusCode);
        responseMap.put("responseStatus", statusMessage);

        return new ResponseEntity<>(responseMap, HttpStatus.valueOf(Integer.parseInt(statusCode)));
    }
}
