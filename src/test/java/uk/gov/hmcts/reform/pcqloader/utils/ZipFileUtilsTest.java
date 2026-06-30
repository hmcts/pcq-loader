package uk.gov.hmcts.reform.pcqloader.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.pcqloader.exceptions.ZipProcessingException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class ZipFileUtilsTest {

    private static final String BLOB_FILENAME_1 = "1579002492_31-08-2020-11-35-10.zip";
    private static final String BAD_ZIP = "1579002493_31-08-2020-11-35-ERROR.zip";

    @Mock
    private File mockedFile;

    @Mock
    private File mockedFolder;

    private File badZip;
    private File actualZip;

    private ZipFileUtils zipFileUtils;

    @BeforeEach
    void setUp() throws FileNotFoundException {
        zipFileUtils = new ZipFileUtils();
        actualZip = ResourceUtils.getFile("classpath:testZipFiles/" + BLOB_FILENAME_1);
        badZip = ResourceUtils.getFile("classpath:testZipFiles/" + BAD_ZIP);
    }

    @Test
    void testConfirmFileCanBeCreated() throws IOException {
        when(mockedFile.getParentFile()).thenReturn(mockedFolder);
        when(mockedFolder.exists()).thenReturn(false);
        when(mockedFolder.mkdirs()).thenReturn(true);
        when(mockedFolder.isDirectory()).thenReturn(true);
        when(mockedFile.createNewFile()).thenReturn(true);
        when(mockedFile.delete()).thenReturn(true);

        boolean result = zipFileUtils.confirmFileCanBeCreated(mockedFile);
        assertTrue(result, "Should be ok to create file");
        verify(mockedFile, times(1)).createNewFile();
        verify(mockedFile, times(1)).delete();
    }

    @Test
    void testConfirmFolderPathCanNotBeCreated() throws IOException {
        when(mockedFile.getParentFile()).thenReturn(mockedFolder);
        when(mockedFolder.exists()).thenReturn(false);
        when(mockedFolder.mkdirs()).thenReturn(false);

        boolean result = zipFileUtils.confirmFileCanBeCreated(mockedFile);
        assertFalse(result, "Should not be ok to create file as path can't be created");
        verify(mockedFile, times(0)).createNewFile();
        verify(mockedFile, times(0)).delete();
        verify(mockedFolder, times(1)).mkdirs();
    }

    @Test
    void testConfirmFileCanNotBeCreated() throws IOException {
        when(mockedFile.getParentFile()).thenReturn(mockedFolder);
        when(mockedFolder.exists()).thenReturn(false);
        when(mockedFolder.mkdirs()).thenReturn(true);
        when(mockedFolder.isDirectory()).thenReturn(true);
        when(mockedFile.exists()).thenReturn(false);
        when(mockedFile.createNewFile()).thenThrow(new IOException());

        boolean result = zipFileUtils.confirmFileCanBeCreated(mockedFile);

        assertFalse(result, "Should not be able to create file");
        verify(mockedFile, times(1)).createNewFile();
        verify(mockedFile, times(0)).delete();
    }

    @Test
    void testConfirmFileCanNotBeDeleted() throws IOException {
        when(mockedFile.getParentFile()).thenReturn(mockedFolder);
        when(mockedFolder.exists()).thenReturn(false);
        when(mockedFolder.mkdirs()).thenReturn(true);
        when(mockedFolder.isDirectory()).thenReturn(true);
        when(mockedFile.exists()).thenReturn(false);
        when(mockedFile.createNewFile()).thenReturn(true);
        when(mockedFile.delete()).thenReturn(false);

        boolean result = zipFileUtils.confirmFileCanBeCreated(mockedFile);

        assertFalse(result, "Should not be able to create file");
        verify(mockedFile, times(1)).createNewFile();
        verify(mockedFile, times(1)).delete();
    }

    @Test
    void testUnzipBlobDownloadZipFileSuccess() {
        File result = zipFileUtils.unzipBlobDownloadZipFile(actualZip);
        List<String> knownFiles = Arrays.asList("metadata.json", "1111001.pdf");
        assertNotNull(result, "Valid folder of unpacked zip returned.");
        assertEquals(2, result.listFiles().length, "Number of files correct in zip.");
        for (File unpackedFile : result.listFiles()) {
            assertTrue(knownFiles.contains(unpackedFile.getName()),"Zip contents correct");
        }
    }

    @Test
    void testUnzipBlobDownloadZipFileError() {
        try {
            zipFileUtils.unzipBlobDownloadZipFile(badZip);
            fail("ZipProcessingException exception should be thrown.");
        } catch (ZipProcessingException zpe) {
            assertNotNull(zpe, "ZipProcessingException exception is thrown.");
        }
    }

    @Test
    void testUnzipBlobDownloadFileMissingError() {
        when(mockedFile.exists()).thenReturn(false);
        try {
            zipFileUtils.unzipBlobDownloadZipFile(mockedFile);
            fail("ZipProcessingException exception should be thrown.");
        } catch (ZipProcessingException zpe) {
            assertNotNull(zpe, "ZipProcessingException exception is thrown.");
        }
    }

    @Test
    void testDeleteFilesFromLocal() {
        // Both files null
        zipFileUtils.deleteFilesFromLocalStorage(null, null);

        //Zip File delete returns false and Folder files array is null
        when(mockedFile.delete()).thenReturn(false);
        when(mockedFolder.listFiles()).thenReturn(null);
        assertThrows(
            NullPointerException.class,
            () -> zipFileUtils.deleteFilesFromLocalStorage(mockedFile, mockedFolder)
        );

        verify(mockedFile, times(1)).delete();
        verify(mockedFolder, times(1)).listFiles();
    }

    @Test
    void testDeleteFilesFromLocalSuccess() {
        //Zip File delete returns true and Folder file delete returns false.
        File testMockedFile = mock(File.class);
        File[] files = {testMockedFile};
        when(mockedFile.delete()).thenReturn(true);
        when(testMockedFile.delete()).thenReturn(false);
        when(mockedFolder.listFiles()).thenReturn(files);
        zipFileUtils.deleteFilesFromLocalStorage(mockedFile, mockedFolder);
        verify(mockedFile, times(1)).delete();
        verify(testMockedFile, times(1)).delete();
        verify(mockedFolder, times(1)).listFiles();
    }

    @Test
    void testDeleteFilesFromLocalAllSuccess() {
        //Zip File delete returns true and folder file delete returns true.
        File testMockedFileSuccess = mock(File.class);
        File[] successFiles = {testMockedFileSuccess};
        when(mockedFile.delete()).thenReturn(true);
        when(testMockedFileSuccess.delete()).thenReturn(true);
        when(mockedFolder.listFiles()).thenReturn(successFiles);
        zipFileUtils.deleteFilesFromLocalStorage(mockedFile, mockedFolder);
        verify(mockedFile, times(1)).delete();
        verify(testMockedFileSuccess, times(1)).delete();
        verify(mockedFolder, times(1)).listFiles();
    }

    @Test
    @SuppressWarnings("PMD.UselessPureMethodCall")
    void testMetaDataFileFail() {
        File[] files = {mockedFile};
        when(mockedFile.getName()).thenReturn("TestFile");

        File metaDataFile = zipFileUtils.getMetaDataFile(files);
        assertNull(metaDataFile, "File should have been null");
        verify(mockedFile, times(1)).getName();
    }

    @Test
    @SuppressWarnings("PMD.UselessPureMethodCall")
    void testMetaDataFileSuccess() {
        File[] files = {mockedFile};
        when(mockedFile.getName()).thenReturn("metadata.json");

        File metaDataFile = zipFileUtils.getMetaDataFile(files);
        assertNotNull(metaDataFile, "File should not have been null");
        verify(mockedFile, times(1)).getName();
    }

}
