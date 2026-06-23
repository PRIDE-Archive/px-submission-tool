package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionPxLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveSubmissionFileExpandsHomeAndTicketPlaceholder() {
        String previousHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        try {
            SubmissionPxLoader loader = new SubmissionPxLoader("~/{ticketId}/submission.px");

            File resolved = loader.resolveSubmissionFile("TICKET-123");

            assertThat(resolved).isEqualTo(tempDir.resolve("TICKET-123/submission.px").toFile());
        } finally {
            System.setProperty("user.home", previousHome);
        }
    }

    @Test
    void loadParsesSubmissionPxMetadataAndFiles() throws Exception {
        Path samplePath = tempDir.resolve("submission.px");
        Files.writeString(samplePath, String.join("\n",
            "MTD\tproject_title\tLoaded ticket submission",
            "MTD\tproject_description\tLoaded ticket submission",
            "MTD\tkeywords\tloaded, ticket",
            "MTD\tsample_processing_protocol\tSample protocol",
            "MTD\tdata_processing_protocol\tData protocol",
            "MTD\texperiment_type\t[PRIDE, PRIDE:0000427, Top-down proteomics, ]",
            "MTD\tsubmission_type\tPARTIAL",
            "MTD\tspecies\t[NEWT, 10090, Mus musculus (Mouse), ]",
            "MTD\tsoftware\t[MS, MS:1003253, DIA-NN, ]",
            "FMH\tfile_id\tfile_type\tfile_path\tfile_mapping",
            "FME\t1\tSEARCH\t/Users/raheela/Downloads/upload-files/4Jul25_Pin1_control.mzTab",
            "COM\tlab_head_country",
            "COM\tVersion:2.11.4",
            "COM\tDataset License:CC0",
            "COM\tOperating System:Mac OS X 26.5.1 (aarch64)",
            "COM\tlab_head_country\tPakistan",
            ""
        ));
        File sample = samplePath.toFile();
        SubmissionPxLoader loader = new SubmissionPxLoader(sample.getAbsolutePath());

        Submission submission = loader.loadForTicket("TICKET-123");

        ProjectMetaData meta = submission.getProjectMetaData();
        assertThat(meta.getProjectTitle()).isEqualTo("Loaded ticket submission");
        assertThat(meta.getSubmissionType()).isEqualTo(SubmissionTypeConstants.PARTIAL);
        assertThat(meta.getSpecies()).extracting(CvParam::getName).containsExactly("Mus musculus (Mouse)");
        assertThat(meta.getSoftwares()).extracting(CvParam::getName).containsExactly("DIA-NN");
        assertThat(submission.getDataFiles()).hasSize(1);
        assertThat(submission.getDataFiles().get(0).getFileType()).isEqualTo(ProjectFileType.SEARCH);
        assertThat(submission.getComments()).contains("Dataset License:CC0");
        assertThat(submission.getComments()).contains("lab_head_country\tPakistan");
        assertThat(submission.getComments()).doesNotContain(
            "lab_head_country",
            "Version:2.11.4",
            "Operating System:Mac OS X 26.5.1 (aarch64)");
    }
}
