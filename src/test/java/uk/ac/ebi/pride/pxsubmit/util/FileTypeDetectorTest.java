package uk.ac.ebi.pride.pxsubmit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FileTypeDetectorTest {

    @TempDir
    Path tempDir;

    private File createTempFile(String name) throws IOException {
        return tempDir.resolve(name).toFile().getAbsoluteFile();
    }

    private File createExistingTempFile(String name) throws IOException {
        File file = createTempFile(name);
        file.createNewFile();
        return file;
    }

    // Extension detection tests

    @Test
    void detectRawFile() throws IOException {
        File file = createExistingTempFile("sample.raw");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.RAW);
    }

    @Test
    void detectMzmlAsRaw() throws IOException {
        File file = createExistingTempFile("sample.mzml");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.RAW);
    }

    @Test
    void detectMzidAsResult() throws IOException {
        File file = createExistingTempFile("sample.mzid");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.RESULT);
    }

    @Test
    void detectMgfAsPeak() throws IOException {
        File file = createExistingTempFile("sample.mgf");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.PEAK);
    }

    @Test
    void detectFastaAsOther() throws IOException {
        File file = createExistingTempFile("database.fasta");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.OTHER);
    }

    @Test
    void detectBlibAsOther() throws IOException {
        File file = createExistingTempFile("library.blib");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.OTHER);
    }

    @Test
    void detectTxtAsSearch() throws IOException {
        File file = createExistingTempFile("evidence.txt");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.SEARCH);
    }

    // SDRF detection tests

    @Test
    void isSdrfFileWithSdrfTsv() {
        assertThat(FileTypeDetector.isSdrfFile("sdrf.tsv")).isTrue();
    }

    @Test
    void isSdrfFileWithExperimentalDesignTsv() {
        assertThat(FileTypeDetector.isSdrfFile("experimental_design.tsv")).isTrue();
    }

    @Test
    void isSdrfFileWithRawFile() {
        assertThat(FileTypeDetector.isSdrfFile("data.raw")).isFalse();
    }

    @Test
    void sdrfFileDetectedAsExperimentalDesign() throws IOException {
        File file = createExistingTempFile("sdrf.tsv");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.EXPERIMENTAL_DESIGN);
    }

    @Test
    void sdrfTakesPriorityOverGenericTsv() throws IOException {
        File file = createExistingTempFile("sdrf-proteomics.tsv");
        assertThat(FileTypeDetector.detectFileType(file)).isEqualTo(ProjectFileType.EXPERIMENTAL_DESIGN);
    }

    // Helper method tests

    @Test
    void isFastaFileTrue() throws IOException {
        File file = createTempFile("sequences.fasta");
        assertThat(FileTypeDetector.isFastaFile(file)).isTrue();
    }

    @Test
    void isFastaFileNull() {
        assertThat(FileTypeDetector.isFastaFile(null)).isFalse();
    }

    @Test
    void isSpectralLibraryTrue() throws IOException {
        File file = createTempFile("library.blib");
        assertThat(FileTypeDetector.isSpectralLibrary(file)).isTrue();
    }

    @Test
    void isSpectralLibraryNull() {
        assertThat(FileTypeDetector.isSpectralLibrary(null)).isFalse();
    }

    // Display name tests

    @Test
    void getDisplayNameRaw() {
        assertThat(FileTypeDetector.getDisplayName(ProjectFileType.RAW)).isEqualTo("RAW Files");
    }

    @Test
    void getDisplayNameResult() {
        assertThat(FileTypeDetector.getDisplayName(ProjectFileType.RESULT)).isEqualTo("STANDARD File Formats");
    }

    @Test
    void getDisplayNameSearch() {
        assertThat(FileTypeDetector.getDisplayName(ProjectFileType.SEARCH)).isEqualTo("ANALYSIS Files");
    }

    @Test
    void getDisplayNamePeak() {
        assertThat(FileTypeDetector.getDisplayName(ProjectFileType.PEAK)).isEqualTo("Peak Lists");
    }

    // Color tests

    @Test
    void getColorReturnsHashPrefixedStrings() {
        assertThat(FileTypeDetector.getColor(ProjectFileType.RAW)).startsWith("#");
        assertThat(FileTypeDetector.getColor(ProjectFileType.RESULT)).startsWith("#");
        assertThat(FileTypeDetector.getColor(ProjectFileType.SEARCH)).startsWith("#");
        assertThat(FileTypeDetector.getColor(ProjectFileType.PEAK)).startsWith("#");
        assertThat(FileTypeDetector.getColor(ProjectFileType.OTHER)).startsWith("#");
    }

    // Mandatory / recommended tests

    @Test
    void isMandatoryRaw() {
        assertThat(FileTypeDetector.isMandatory(ProjectFileType.RAW)).isTrue();
    }

    @Test
    void isMandatoryResultFalse() {
        assertThat(FileTypeDetector.isMandatory(ProjectFileType.RESULT)).isFalse();
    }

    @Test
    void isRecommendedResult() {
        assertThat(FileTypeDetector.isRecommended(ProjectFileType.RESULT)).isTrue();
    }

    @Test
    void isRecommendedSearch() {
        assertThat(FileTypeDetector.isRecommended(ProjectFileType.SEARCH)).isTrue();
    }

    @Test
    void isRecommendedRawFalse() {
        assertThat(FileTypeDetector.isRecommended(ProjectFileType.RAW)).isFalse();
    }

    // Extension set tests

    @Test
    void getExtensionsRawContainsExpected() {
        Set<String> extensions = FileTypeDetector.getExtensions(ProjectFileType.RAW);
        assertThat(extensions).contains("raw", "mzml");
    }

    // Tool detection tests

    @Test
    void detectToolWithEmptyCollection() {
        FileTypeDetector.ToolDetectionResult result = FileTypeDetector.detectTool(Collections.emptyList());
        assertThat(result.tool()).isEqualTo(FileTypeDetector.AnalysisTool.UNKNOWN);
    }

    @Test
    void detectToolWithNullCollection() {
        FileTypeDetector.ToolDetectionResult result = FileTypeDetector.detectTool(null);
        assertThat(result.tool()).isEqualTo(FileTypeDetector.AnalysisTool.UNKNOWN);
    }

    // Null file handling

    @Test
    void detectFileTypeNullFile() {
        assertThat(FileTypeDetector.detectFileType((File) null)).isEqualTo(ProjectFileType.OTHER);
    }
}
