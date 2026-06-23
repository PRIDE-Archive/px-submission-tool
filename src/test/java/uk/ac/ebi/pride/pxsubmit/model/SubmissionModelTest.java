package uk.ac.ebi.pride.pxsubmit.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionModelTest {

    @Test
    void setSubmissionClearsFieldsMissingFromReplacementSubmission() {
        SubmissionModel model = new SubmissionModel();
        Submission loadedSubmission = new Submission();
        ProjectMetaData loadedMetadata = loadedSubmission.getProjectMetaData();
        loadedMetadata.setProjectTitle("Loaded from ticket");
        loadedMetadata.addSpecies(new CvParam("NEWT", "NCBITaxon:9606", "Homo sapiens", null));
        loadedSubmission.addDataFile(new DataFile(new File("loaded.raw"), ProjectFileType.RAW));

        model.setSubmission(loadedSubmission);
        model.setSubmission(new Submission());

        assertThat(model.getProjectTitle()).isNull();
        assertThat(model.getSpecies()).isEmpty();
        assertThat(model.getFiles()).isEmpty();
    }

    @Test
    void setSubmissionRestoresLabHeadCountryFromNormalizedComment() {
        SubmissionModel model = new SubmissionModel();
        Submission loadedSubmission = new Submission();
        loadedSubmission.setComments(List.of("lab_head_country\tPakistan"));

        model.setSubmission(loadedSubmission);

        assertThat(model.getLabHeadCountry()).isEqualTo("Pakistan");
    }
}
