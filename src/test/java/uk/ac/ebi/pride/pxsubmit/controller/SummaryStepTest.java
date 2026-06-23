package uk.ac.ebi.pride.pxsubmit.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryStepTest {

    @TempDir
    Path tempDir;

    @Test
    void writeSubmissionPxFileDoesNotDuplicateLoadedLabHeadCountryComment() throws Exception {
        SubmissionModel model = new SubmissionModel();
        model.setLabHeadCountry("Pakistan");
        model.getSubmission().setComments(List.of(
            "lab_head_country",
            "lab_head_country\tPakistan",
            "Version:2.11.4",
            "Dataset License:CC0",
            "Operating System:old"
        ));

        Path output = tempDir.resolve("submission.px");

        SummaryStep.writeSubmissionPxFile(model, output.toFile());

        List<String> countryLines = Files.readAllLines(output).stream()
            .filter(line -> line.startsWith("COM\tlab_head_country"))
            .toList();
        assertThat(countryLines).containsExactly("COM\tlab_head_country\tPakistan");
        assertThat(Files.readString(output)).contains("COM\tDataset License:CC0");
    }
}
