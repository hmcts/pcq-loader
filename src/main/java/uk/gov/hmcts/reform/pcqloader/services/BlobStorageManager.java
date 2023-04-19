package uk.gov.hmcts.reform.pcqloader.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcqloader.config.BlobStorageProperties;
import uk.gov.hmcts.reform.pcqloader.exceptions.BlobProcessingException;
import uk.gov.hmcts.reform.pcqloader.utils.ZipFileUtils;

import java.io.File;
import java.io.IOException;
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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public List<String> collectBlobFileNamesFromContainer(BlobContainerClient blobContainerClient) {
        List<String> zipFilenames = new ArrayList<>();
        for (BlobItem blob : blobContainerClient.listBlobsByHierarchy(BLOB_CONTAINER_FOLDER)) {
            if (!blob.isDeleted() && null == blob.isPrefix()) {
                String fileName = FilenameUtils.getName(blob.getName());
                if (fileName == null || fileName.isEmpty()) {
                    log.error("Unable to retrieve blob filename from container: {}", blob.getName());
                } else {
                    zipFilenames.add(fileName);
                }
            }
        }

        Collections.shuffle(zipFilenames);
        return zipFilenames;
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis","PMD.LawOfDemeter"})
    public File downloadFileFromBlobStorage(BlobContainerClient blobContainerClient, String blobName) {
        log.debug("Downloading blob name {} to {} path",
                  blobName, blobStorageProperties.getBlobStorageDownloadPath());
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
        log.info("Uploading to Blob storage as blob: {}", blobClient.getBlobUrl());
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

        destinationClient.beginCopy(sourceClient.getBlobUrl(), null);
        sourceClient.delete();
        log.info("Moved file {} to the Rejected Container", fileName);
    }

    public void moveFileToProcessedFolder(String fileName, BlobContainerClient sourceContainer) {
        BlobClient sourceClient = sourceContainer.getBlobClient(fileName);
        BlobClient destinationClient = sourceContainer.getBlobClient(blobStorageProperties.getProcessedFolderName()
                                                                           + File.separator + fileName);
        destinationClient.beginCopy(sourceClient.getBlobUrl(), null);
        sourceClient.delete();
        log.info("Moved file {} to the Processed Folder", fileName);
    }
}
