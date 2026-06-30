package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.service.WebSubIncomingFileValidationService.FileTransferEntry;
import uk.ac.ebi.pride.pxsubmit.service.WebSubIncomingFileValidationService.FileValidateProgress;
import uk.ac.ebi.pride.pxsubmit.service.WebSubIncomingFileValidationService.ValidationResponse;
import uk.ac.ebi.pride.pxsubmit.service.WebSubIncomingFileValidationService.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebSubIncomingFileValidationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void responseWithInvalidProcessedFileMarksValidationFailed() throws Exception {
        Path file = Files.writeString(tempDir.resolve("bad.raw"), "content");
        DataFile dataFile = new DataFile();
        dataFile.setFile(file.toFile());

        FileValidateProgress progress = new FileValidateProgress();
        progress.setValid(false);
        progress.setSeverity("ERROR");
        progress.setMessage("Unsupported file");

        FileTransferEntry entry = new FileTransferEntry();
        entry.setName(file.getFileName().toString());
        entry.setProgress(progress);

        ValidationResponse response = new ValidationResponse();
        response.setFinished(true);
        response.setState("FINISHED");
        response.setProcessedFiles(List.of(entry));

        ValidationResult result = ValidationResult.from(response, List.of(dataFile));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).containsExactly("bad.raw: Unsupported file");
        assertThat(result.fileValidByPath()).containsEntry(file.toFile().getAbsolutePath(), false);
    }

    @Test
    void responseWithWarningKeepsValidationPassing() throws Exception {
        Path file = Files.writeString(tempDir.resolve("ok.raw"), "content");
        DataFile dataFile = new DataFile();
        dataFile.setFile(file.toFile());

        FileValidateProgress progress = new FileValidateProgress();
        progress.setValid(true);
        progress.setSeverity("WARNING");
        progress.setMessage("Check metadata");

        FileTransferEntry entry = new FileTransferEntry();
        entry.setName(file.getFileName().toString());
        entry.setProgress(progress);

        ValidationResponse response = new ValidationResponse();
        response.setFinished(true);
        response.setState("FINISHED");
        response.setProcessedFiles(List.of(entry));

        ValidationResult result = ValidationResult.from(response, List.of(dataFile));

        assertThat(result.valid()).isTrue();
        assertThat(result.warnings()).containsExactly("ok.raw: Check metadata");
        assertThat(result.fileValidByPath()).containsEntry(file.toFile().getAbsolutePath(), true);
    }
}
