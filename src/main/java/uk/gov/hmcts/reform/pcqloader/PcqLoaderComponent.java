package uk.gov.hmcts.reform.pcqloader;

import com.azure.storage.blob.BlobContainerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.utils.PcqUtils;
import uk.gov.hmcts.reform.pcqloader.helper.PayloadMappingHelper;
import uk.gov.hmcts.reform.pcqloader.services.BlobStorageManager;
import uk.gov.hmcts.reform.pcqloader.services.PcqBackendService;
import uk.gov.hmcts.reform.pcqloader.utils.LogSummaryUtils;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PcqLoaderComponent {

    private static final int MAX_RETRIES = 3;
    private static final String ERROR_SUFFIX = "_Erred";
    private static final String CRATED_SUFFIX = "_Created";


    private final Map<String, Integer> serviceSummaryMap = new ConcurrentHashMap<>();

    @Value("${apiExecutionThreadDelay:1000}")
    private int threadDelay;

    private final BlobStorageManager blobStorageManager;

    private final PayloadMappingHelper payloadMappingHelper;

    private final PcqBackendService pcqBackendService;

    private final ZipFileUtils fileUtil;


    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    public void execute() {

        // Step 1. Connect and Authenticate with the PCQ Azure Blob Storage Account.
        BlobContainerClient blobContainerClient = blobStorageManager.getPcqContainer();
        Assert.isTrue(blobContainerClient.exists(), "Can't connect to Blob Storage.");

        // Step 2. Check for zip files in the Pcq container.
        List<String> blobZipNamesList = blobStorageManager.collectBlobFileNamesFromContainer(blobContainerClient);
        for (String tmpZipFileName : blobZipNamesList) {

            File blobZipDirectory = null;
            File unzippedFiles = null;
            String jurisdiction = "";
            try {
                // Step 4. Download the zip file to local storage.
                blobZipDirectory = blobStorageManager.downloadFileFromBlobStorage(blobContainerClient, tmpZipFileName);

                // Step 5. Retrieve the DCN Number from the Zip File Name. Pass the actual zip file name in the
                // method call below
                final String fileDcnNumber = PcqUtils.extractDcnNumberFromFile(tmpZipFileName);
                log.info("Starting to process file {}", fileDcnNumber);

                // Step 6. Unzip the zip file
                unzippedFiles = fileUtil.unzipBlobDownloadZipFile(blobZipDirectory);

                // Step 7. Read the file and generate the mapping to the PcqAnswers object.
                File metaDataFile = fileUtil.getMetaDataFile(Objects.requireNonNull(unzippedFiles.listFiles()));
                if (metaDataFile == null) {
                    log.error("metadata.json file not found, moving the zip file to Rejected container");
                    blobStorageManager.moveFileToRejectedContainer(tmpZipFileName, blobContainerClient);
                    incrementServiceCount(jurisdiction + ERROR_SUFFIX);
                } else {
                    String jsonMetaData = jsonStringFromFile(metaDataFile);
                    PcqAnswerRequest mappedAnswers = payloadMappingHelper.mapPayLoadToPcqAnswers(
                        jsonMetaData);

                    if (mappedAnswers == null) {
                        log.error("Mapping failed, moving the zip file to Rejected container");
                        blobStorageManager.moveFileToRejectedContainer(tmpZipFileName, blobContainerClient);
                        incrementServiceCount(jurisdiction + ERROR_SUFFIX);
                    } else {
                        //Step 8. Invoke the back-end API
                        jurisdiction = mappedAnswers.getServiceId();
                        invokeSubmitAnswers(mappedAnswers, tmpZipFileName, blobContainerClient);

                    }
                }
            } catch (Exception ioe) {
                log.error("Error during processing " + ioe.getMessage(), ioe);
                incrementServiceCount(jurisdiction + ERROR_SUFFIX);
            } finally {
                fileUtil.deleteFilesFromLocalStorage(blobZipDirectory, unzippedFiles);
            }
        }
        LogSummaryUtils.logSummary(serviceSummaryMap);
    }


    @SuppressWarnings({"unchecked","PMD.CognitiveComplexity"})
    private void invokeSubmitAnswers(PcqAnswerRequest mappedAnswers, String tmpZipFileName,
                                     BlobContainerClient sourceContainer) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                ResponseEntity<Map<String, String>> responseEntity = pcqBackendService
                    .submitAnswers(mappedAnswers);

                if (responseEntity.getStatusCode() == HttpStatus.CREATED
                    || responseEntity.getStatusCode() == HttpStatus.CONFLICT) {
                    log.info(
                        "File {} with DCN {} processed successfully, moving file to processed folder.",
                        tmpZipFileName,mappedAnswers.getDcnNumber()
                    );
                    blobStorageManager.moveFileToProcessedFolder(tmpZipFileName, sourceContainer);
                    incrementServiceCount(mappedAnswers.getServiceId() + CRATED_SUFFIX);
                } else {
                    log.error("Back-end API call returned invalid response, moving file to rejected "
                                  + "container - Error code {} for DCN {}",
                              responseEntity.getStatusCode(), mappedAnswers.getDcnNumber());
                    blobStorageManager.moveFileToRejectedContainer(tmpZipFileName, sourceContainer);
                    incrementServiceCount(mappedAnswers.getServiceId() + ERROR_SUFFIX);
                }
                retryCount = MAX_RETRIES;

            } catch (ExternalApiException apiException) {
                log.error(
                    "API Exception occurred during execution {} ", apiException.getMessage(),
                    apiException
                );
                if (retryCount < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(threadDelay);
                        log.info("Re-trying to process file {}", tmpZipFileName);
                    } catch (InterruptedException ie) {
                        log.info("Interrupted Exception is thrown : ", ie);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    incrementServiceCount(mappedAnswers.getServiceId() + ERROR_SUFFIX);
                }
                retryCount++;
            }
        }
    }

    private String jsonStringFromFile(File file) throws IOException {
        return fileUtil.readAllBytesFromFile(file);
    }

    private void incrementServiceCount(String service) {

        if (serviceSummaryMap.get(service) == null) {
            serviceSummaryMap.put(service, 1);
        } else {
            int count = serviceSummaryMap.get(service) + 1;
            serviceSummaryMap.put(service, count);
        }
    }
}
