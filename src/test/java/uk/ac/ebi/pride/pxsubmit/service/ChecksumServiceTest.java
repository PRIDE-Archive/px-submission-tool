package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChecksumServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesChecksumFileAsFileNameTabChecksum() throws Exception {
        DataFile rawFile = dataFile("sample.raw");
        Map<DataFile, String> checksums = new LinkedHashMap<>();
        checksums.put(rawFile, "abc123");

        File checksumFile = ChecksumService.writeChecksumFile(
                checksums,
                List.of(rawFile),
                tempDir.toFile());

        assertThat(Files.readString(checksumFile.toPath()).trim())
                .isEqualTo("sample.raw\tabc123");
    }

    @Test
    void rejectsMissingChecksumForSelectedUploadFile() {
        DataFile selectedFile = dataFile("sample.raw");

        ChecksumService.ChecksumValidationResult result =
                ChecksumService.validateChecksumCoverage(List.of(selectedFile), Map.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .contains("Missing checksum entry for selected file: sample.raw");
    }

    @Test
    void rejectsChecksumEntryForFileNotSelectedForUpload() {
        DataFile selectedFile = dataFile("selected.raw");
        DataFile extraFile = dataFile("extra.raw");
        Map<DataFile, String> checksums = Map.of(extraFile, "abc123");

        ChecksumService.ChecksumValidationResult result =
                ChecksumService.validateChecksumCoverage(List.of(selectedFile), checksums);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .contains("Missing checksum entry for selected file: selected.raw")
                .contains("Checksum entry does not match a selected upload file: extra.raw");
    }

    @Test
    void writeFailsWhenChecksumCoverageDoesNotMatchSelectedFiles() {
        DataFile selectedFile = dataFile("selected.raw");
        DataFile extraFile = dataFile("extra.raw");

        assertThatThrownBy(() -> ChecksumService.writeChecksumFile(
                Map.of(extraFile, "abc123"),
                List.of(selectedFile),
                tempDir.toFile()))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Checksum coverage validation failed");
    }

    @Test
    void rewriteRemovesFilesThatAreNoLongerSelectedForUpload() throws Exception {
        DataFile keptFile = dataFile("kept.raw");
        DataFile removedFile = dataFile("removed.raw");

        ChecksumService.writeChecksumFile(
                Map.of(keptFile, "kept123", removedFile, "removed123"),
                List.of(keptFile, removedFile),
                tempDir.toFile());

        File rewrittenChecksumFile = ChecksumService.writeChecksumFile(
                Map.of(keptFile, "kept123"),
                List.of(keptFile),
                tempDir.toFile());

        assertThat(Files.readAllLines(rewrittenChecksumFile.toPath()))
                .containsExactly("kept.raw\tkept123");
    }

    private DataFile dataFile(String name) {
        DataFile dataFile = new DataFile();
        dataFile.setFile(tempDir.resolve(name).toFile());
        return dataFile;
    }
}
