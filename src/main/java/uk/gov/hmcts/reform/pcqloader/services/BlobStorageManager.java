package uk.gov.hmcts.reform.pcqloader.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.CopyStatusType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcqloader.config.BlobStorageProperties;
import uk.gov.hmcts.reform.pcqloader.exceptions.BlobProcessingException;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BlobStorageManager {

    private static final String BLOB_CONTAINER_FOLDER = "";

    private final BlobServiceClient blobServiceClient;

    private final BlobStorageProperties blobStorageProperties;

    private final ZipFileUtils zipFileUtils;

    public BlobStorageManager(
        BlobStorageProperties blobStorageProperties,
        BlobServiceClient blobServiceClient,
        ZipFileUtils zipFileUtils
    ) {
        this.blobStorageProperties = blobStorageProperties;
        this.blobServiceClient = blobServiceClient;
        this.zipFileUtils = zipFileUtils;
    }

    public BlobContainerClient getContainer(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    public BlobContainerClient getPcqContainer() {
        return getContainer(blobStorageProperties.getBlobPcqContainer());
    }

    public BlobContainerClient getRejectedPcqContainer() {
        return getContainer(blobStorageProperties.getBlobPcqRejectedContainer());
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis","PMD.LawOfDemeter"})
    public List<String> collectBlobFileNamesFromContainer(BlobContainerClient blobContainerClient) {
        List<String> zipFilenames = new ArrayList<>();
        for (BlobItem blob : blobContainerClient.listBlobsByHierarchy(BLOB_CONTAINER_FOLDER)) {
            if (!blob.isDeleted() && Boolean.FALSE.equals(blob.isPrefix())) {
                String fileName = FilenameUtils.getName(blob.getName());
                if (StringUtils.isEmpty(fileName)) {
                    log.error("Unable to retrieve blob filename from container: {}", blob.getName());
                } else {
                    zipFilenames.add(fileName);
                }
            }
        }

        Collections.shuffle(zipFilenames);
        return Collections.unmodifiableList(zipFilenames);
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis","PMD.LawOfDemeter"})
    public File downloadFileFromBlobStorage(BlobContainerClient blobContainerClient, String blobName) {
        String filePath = blobStorageProperties.getBlobStorageDownloadPath() + File.separator + blobName;
        File localFile = new File(filePath);

        try {
            if (Boolean.TRUE.equals(zipFileUtils.confirmFileCanBeCreated(localFile))) {
                blobContainerClient.getBlobClient(blobName).downloadToFile(filePath, true);
                if (localFile.exists()) {
                    log.info("Succeessfully downloaded blob file to path: {}", localFile.getPath());
                    return localFile;
                }
            } else {
                throw new IOException("Unable to write blob file to filesystem");
            }
        } catch (Exception exp) {
            log.error("Error downloading {} from Blob Storage", blobName);
            throw new BlobProcessingException("Unable to download blob file.", exp);
        }

        log.error("Error downloading {} from Blob Storage", blobName);
        throw new BlobProcessingException("Unknown error downloading blob file.");
    }

    public void uploadFileToBlobStorage(BlobContainerClient blobContainerClient, String filePath) {
        File localFileUpload = new File(filePath);
        log.debug("Uploading file {} to {} container",
                 localFileUpload.getName(), blobContainerClient.getBlobContainerName());
        BlobClient blobClient = blobContainerClient.getBlobClient(localFileUpload.getName());
        log.debug("Uploading to Blob storage as blob: {}", blobClient.getBlobUrl());
        blobClient.uploadFromFile(filePath);
    }

    public BlobContainerClient createContainer(String containerName) {
        if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
            return blobServiceClient.getBlobContainerClient(containerName);
        }
        return blobServiceClient.createBlobContainer(containerName);
    }

    public void deleteContainer(String containerName) {
        blobServiceClient.deleteBlobContainer(containerName);
    }

    public void moveFileToRejectedContainer(String fileName, BlobContainerClient sourceContainer) {
        BlobContainerClient rejectedContainer = createContainer(blobStorageProperties.getBlobPcqRejectedContainer());
        BlobClient sourceClient = sourceContainer.getBlobClient(fileName);
        BlobClient destinationClient = rejectedContainer.getBlobClient(fileName);

        try {
            copyBlobAndDeleteSource(fileName, sourceClient, destinationClient);
            log.info("Moved file {} to the Rejected Container", fileName);
        } catch (Exception ex) {
            log.error("Error moving file {} to rejected container: {}", fileName, ex.getMessage(), ex);
            throw new BlobProcessingException("Failed to move blob to rejected container: " + fileName
                                                  + " - " + ex.getMessage(), ex);
        }
    }

    public void moveFileToProcessedFolder(String fileName, BlobContainerClient sourceContainer) {
        BlobClient sourceClient = sourceContainer.getBlobClient(fileName);
        String processedPath = blobStorageProperties.getProcessedFolderName() + File.separator + fileName;
        BlobClient destinationClient = sourceContainer.getBlobClient(processedPath);

        try {
            copyBlobAndDeleteSource(fileName, sourceClient, destinationClient);
            log.info("Moved file {} to the Processed Folder", fileName);
        } catch (Exception ex) {
            log.error("Error moving file {} to processed folder: {}", fileName, ex.getMessage(), ex);
            throw new BlobProcessingException("Failed to move blob to processed folder: " + fileName, ex);
        }
    }

    private void copyBlobAndDeleteSource(String fileName, BlobClient sourceClient, BlobClient destinationClient) {
        var pollResponse = destinationClient.beginCopy(sourceClient.getBlobUrl(), null);
        BlobCopyInfo copyInfo = pollResponse
            .waitForCompletion(Duration.ofMillis(blobStorageProperties.getBlobCopyTimeoutInMillis()))
            .getValue();
        CopyStatusType status = copyInfo != null ? copyInfo.getCopyStatus() : null;
        if (status != CopyStatusType.SUCCESS) {
            throw new BlobProcessingException(String.format("Copy failed for file %s with status %s", fileName,
                                                            status));
        }
        sourceClient.delete();
    }
}
