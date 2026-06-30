package uk.gov.hmcts.reform.pcqloader.utils;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressWarnings("HideUtilityClassConstructor")
public class BlobStorageInitialisation {

    private static final String DEV_CONNECTION_STRING = "UseDevelopmentStorage=true";

    private static final String DEV_PCQ_CONTAINER_NAME = "pcq";
    private static final String DEV_PCQ_CONTAINER_REJECTED_NAME = "pcq-rejected";

    private static final String SAMPLE_DOCUMENT_PATH_1
        = "src/integrationTest/resources/blobTestFiles/1579002492_31-08-2020-11-35-10.zip";
    private static final String SAMPLE_DOCUMENT_PATH_2
        = "src/integrationTest/resources/blobTestFiles/1579002493_31-08-2020-11-48-42.zip";

    private static List<String> availableContaimers = new ArrayList<>();

    public static void main(String[] args) {
        log.info("Connecting to Azurite Blob Storage");

        // Connect to development auzrite blob storage
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(DEV_CONNECTION_STRING)
            .buildClient();

        // Clear out available containers
        blobServiceClient.listBlobContainers()
            .forEach(container -> availableContaimers.add(container.getName()));
        availableContaimers.forEach(blobServiceClient::deleteBlobContainer);
        log.info("Creating {} container", DEV_PCQ_CONTAINER_NAME);
        blobServiceClient.createBlobContainer(DEV_PCQ_CONTAINER_NAME);
        log.info("Creating {} container", DEV_PCQ_CONTAINER_REJECTED_NAME);
        blobServiceClient.createBlobContainer(DEV_PCQ_CONTAINER_REJECTED_NAME);

        // Add sample documents to pcq container
        BlobContainerClient blobContainerClient =  blobServiceClient.getBlobContainerClient(DEV_PCQ_CONTAINER_NAME);
        uploadFileToBlobStorage(blobContainerClient, SAMPLE_DOCUMENT_PATH_1);
        uploadFileToBlobStorage(blobContainerClient, SAMPLE_DOCUMENT_PATH_2);

        BlobServiceSasSignatureValues policy = new BlobServiceSasSignatureValues(
            OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(300_000),
            BlobContainerSasPermission.parse("cwl"));

        log.info("TEST URL: {}?comp=list&restype=container&{}",
                 blobContainerClient.getBlobContainerUrl(),
                 blobContainerClient.generateSas(policy));
    }

    private static void uploadFileToBlobStorage(BlobContainerClient blobContainerClient, String filePath) {
        File localFileUpload = new File(filePath);
        log.info("Uploading file {} to {} container",
                 localFileUpload.getAbsoluteFile(), blobContainerClient.getBlobContainerName());
        BlockBlobClient blockBlobClient =
            blobContainerClient.getBlobClient(localFileUpload.getName()).getBlockBlobClient();
        log.info("Uploading to Blob storage as blob: {}", blobContainerClient.getBlobContainerUrl());
        try {
            byte[] fileBytes = FileUtils.readFileToByteArray(localFileUpload);
            ByteArrayInputStream dataStream = new ByteArrayInputStream(fileBytes);
            blockBlobClient.upload(dataStream, fileBytes.length, true);
        } catch (IOException ioe) {
            log.error("Unable to upload file to blob storage.", ioe);
        }
    }
}
