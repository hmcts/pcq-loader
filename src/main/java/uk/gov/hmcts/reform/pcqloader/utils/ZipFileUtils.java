package uk.gov.hmcts.reform.pcqloader.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcqloader.exceptions.ZipProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
public class ZipFileUtils {

    private static final String ZIP_FOLDER_POSTFIX = ".zip";

    private static final String METADATA_FILE_NAME = "metadata.json";

    public static final double THRESHOLD_RATIO = 10;

    public Boolean confirmFileCanBeCreated(File blobFile) {
        File blobFolder = blobFile.getParentFile();
        if ((blobFolder.exists() || blobFolder.mkdirs()) && blobFolder.isDirectory()) {
            try {
                if (!blobFile.exists()) {
                    return blobFile.createNewFile() && cleanUp(blobFile);
                }
                return true;

            } catch (IOException e) {
                log.error(
                    "Unable to confirm if {} can be created.",
                    blobFile.getName()
                );
            }
        }
        return false;
    }

    public File unzipBlobDownloadZipFile(File blobDownload) {
        if (blobDownload.exists()
            && blobDownload.isFile()
            && blobDownload.getPath().toLowerCase(Locale.ENGLISH).endsWith(ZIP_FOLDER_POSTFIX)) {
            try (ZipFile zipFile = new ZipFile(blobDownload.getAbsoluteFile())) {
                File outputDir = new File(FilenameUtils.removeExtension(blobDownload.getPath()));
                initialiseDirectory(outputDir);
                Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
                while (zipEntries.hasMoreElements()) {
                    ZipEntry ze = zipEntries.nextElement();
                    String simpleName = FilenameUtils.getName(ze.getName());
                    if (!ze.isDirectory() && simpleName.charAt(0) != '.') {
                        log.info("Found zip content: " + ze.getName());
                        checkUnzipFileSize(zipFile,ze,outputDir,simpleName);
                    }
                }
                return outputDir;
            } catch (IOException ioe) {
                throw new ZipProcessingException("Unable to read zip file " + blobDownload.getName(), ioe);
            }

        } else {
            throw new ZipProcessingException("Unable to process blob zip file " + blobDownload.getName());
        }
    }

    public void deleteFilesFromLocalStorage(File zipFile, File unzippedFiles) {
        if (zipFile != null && !cleanUp(zipFile)) {
            log.warn("Zip file {} not removed from local storage.", zipFile.getName());
        }

        if (unzippedFiles != null) {
            File[] files = unzippedFiles.listFiles();
            for (File file : Objects.requireNonNull(files)) {
                if (!cleanUp(file)) {
                    log.warn("File {} not removed from local storage.", file.getName());
                }
            }
            if (!cleanUp(unzippedFiles)) {
                log.warn("File {} not removed from local storage.", unzippedFiles.getName());
            }
        }

    }

    public File getMetaDataFile(File... unzippedFiles) {
        File metaDataFile = null;
        for (File file : unzippedFiles) {
            if (METADATA_FILE_NAME.equals(file.getName())) {
                metaDataFile = file;
            }
        }

        return metaDataFile;
    }

    public String readAllBytesFromFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    private void initialiseDirectory(File directory) throws IOException {
        if (directory.exists() || directory.mkdirs()) {
            FileUtils.cleanDirectory(directory);
        }
    }

    public void checkUnzipFileSize(ZipFile zipFile, ZipEntry ze, File outputDir, String simpleName) throws IOException {
        var fileToCreate = outputDir.toPath().resolve(simpleName);
        byte[] buffer = new byte[1248];
        Path outPath = Paths.get(fileToCreate.toString());
        try (InputStream in = zipFile.getInputStream(ze); OutputStream out = Files.newOutputStream(outPath)) {
            int bytes;
            double totalSizeEntry = 0;
            bytes = in.read(buffer);
            while (bytes > 0) { // Compliant
                out.write(buffer, 0, bytes);
                totalSizeEntry += bytes;
                bytes = in.read(buffer);
                double compressionRatio = totalSizeEntry / ze.getCompressedSize();
                if (compressionRatio > THRESHOLD_RATIO) {
                    throw new ZipProcessingException(
                        "Ratio between compressed and uncompressed data is highly suspicious "
                            + ze.getName());
                }
            }
        } catch (IOException e) {
            log.error("An error occurred while unzipping file from blob storage",e);
            throw new ZipProcessingException("Unable to unpack zip file "
                                                 + ze.getName(), e);
        }
        Files.copy(zipFile.getInputStream(ze), fileToCreate, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean cleanUp(File file) {
        return file.delete();
    }
}
