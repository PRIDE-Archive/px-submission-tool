package uk.ac.ebi.pride.pxsubmit.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileNameValidatorTest {

    @Test
    void acceptsUploadSafeFileNames() {
        List<String> fileNames = List.of(
            "sample.raw",
            "sample-1.raw",
            "sample_1.mzML",
            "A123.raw",
            "proteinGroups.txt",
            "sample.wiff.scan"
        );

        for (String fileName : fileNames) {
            assertThat(FileNameValidator.isValidSubmissionFileName(fileName)).isTrue();
        }
    }

    @Test
    void rejectsSpacesAndSpecialCharacters() {
        List<String> fileNames = List.of(
            " ",
            ".",
            "..",
            "sample raw.raw",
            "sample(raw).raw",
            "sample+raw.raw",
            "sample#raw.raw",
            "sample/raw.raw",
            "sample:raw.raw",
            "sample@raw.raw"
        );

        assertThat(FileNameValidator.isValidSubmissionFileName((String) null)).isFalse();
        assertThat(FileNameValidator.isValidSubmissionFileName("")).isFalse();
        for (String fileName : fileNames) {
            assertThat(FileNameValidator.isValidSubmissionFileName(fileName)).isFalse();
        }
    }

    @Test
    void findsInvalidFileNamesInCollection() {
        File validFile = new File("/tmp/sample.raw");
        File invalidFile = new File("/tmp/sample raw.raw");

        assertThat(FileNameValidator.findInvalidSubmissionFileNames(List.of(validFile, invalidFile)))
                .containsExactly(invalidFile);
    }
}
