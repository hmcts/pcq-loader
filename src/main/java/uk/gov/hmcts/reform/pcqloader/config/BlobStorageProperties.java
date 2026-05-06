package uk.gov.hmcts.reform.pcqloader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
public class BlobStorageProperties {

    private String blobStorageDownloadPath;

    private String blobPcqContainer;

    private String blobPcqRejectedContainer;

    private String processedFolderName;

    private long blobCopyTimeoutInMillis;

    public String getBlobPcqContainer() {
        return blobPcqContainer;
    }

    public void setBlobPcqContainer(String blobPcqContainer) {
        this.blobPcqContainer = blobPcqContainer;
    }

    public String getBlobStorageDownloadPath() {
        return blobStorageDownloadPath;
    }

    public void setBlobStorageDownloadPath(String blobStorageDownloadPath) {
        this.blobStorageDownloadPath = blobStorageDownloadPath;
    }

    public String getBlobPcqRejectedContainer() {
        return blobPcqRejectedContainer;
    }

    public void setBlobPcqRejectedContainer(String blobPcqRejectedContainer) {
        this.blobPcqRejectedContainer = blobPcqRejectedContainer;
    }

    public String getProcessedFolderName() {
        return processedFolderName;
    }

    public void setProcessedFolderName(String processedFolderName) {
        this.processedFolderName = processedFolderName;
    }

    public long getBlobCopyTimeoutInMillis() {
        return blobCopyTimeoutInMillis;
    }

    public void setBlobCopyTimeoutInMillis(long blobCopyTimeoutInMillis) {
        this.blobCopyTimeoutInMillis = blobCopyTimeoutInMillis;
    }
}
