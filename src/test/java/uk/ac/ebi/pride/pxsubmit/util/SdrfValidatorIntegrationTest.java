package uk.ac.ebi.pride.pxsubmit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ebi.pride.sdrf.validate.model.SDRFContent;
import uk.ac.ebi.pride.sdrf.validate.validation.SDRFParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SDRF validation using the jsdrf library parser.
 */
class SdrfValidatorIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void validSdrfFileIsParsedSuccessfully() throws Exception {
        String content = String.join("\t",
                "source name",
                "characteristics[organism]",
                "characteristics[organism part]",
                "characteristics[disease]",
                "assay name",
                "comment[instrument]",
                "comment[data file]",
                "comment[label]",
                "comment[fraction identifier]",
                "comment[technical replicate]"
        ) + "\n" + String.join("\t",
                "sample1",
                "Homo sapiens",
                "liver",
                "normal",
                "run1",
                "Q Exactive",
                "sample1.raw",
                "label free sample",
                "1",
                "1"
        ) + "\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        assertThat(sdrfContent).isNotNull();
        assertThat(sdrfContent.getSdrfColumns()).hasSize(10);
        assertThat(sdrfContent.getSdrfRows()).hasSize(1);
    }

    @Test
    void parserExtractsColumnsCorrectly() throws Exception {
        String content = "source name\tcharacteristics[organism]\tassay name\n" +
                          "sample1\tHomo sapiens\trun1\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        List<String> columnNames = sdrfContent.getSdrfColumns().stream()
                .map(col -> col.getName().toLowerCase().trim())
                .toList();

        assertThat(columnNames).contains("source name", "characteristics[organism]", "assay name");
    }

    @Test
    void missingRequiredColumnsDetectable() throws Exception {
        // Only has assay name, missing source name and characteristics[organism]
        String content = "assay name\tcomment[data file]\n" +
                          "run1\tsample1.raw\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        List<String> columnNames = sdrfContent.getSdrfColumns().stream()
                .map(col -> col.getName().toLowerCase().trim())
                .toList();

        assertThat(columnNames).doesNotContain("source name");
        assertThat(columnNames.stream().noneMatch(c -> c.contains("characteristics[organism]"))).isTrue();
    }

    @Test
    void emptyFileThrowsException() throws IOException {
        File sdrfFile = createSdrfFile("empty.tsv", "");

        SDRFParser parser = new SDRFParser();
        assertThatThrownBy(() -> parser.getSDRFContent(sdrfFile.getAbsolutePath()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void headerOnlyFileProducesNoRows() throws Exception {
        String content = "source name\tcharacteristics[organism]\n";

        File sdrfFile = createSdrfFile("header_only.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        assertThat(sdrfContent.getSdrfColumns()).isNotEmpty();
        assertThat(sdrfContent.getSdrfRows()).isEmpty();
    }

    @Test
    void multipleRowsParsedCorrectly() throws Exception {
        String content = "source name\tcharacteristics[organism]\n" +
                          "sample1\tHomo sapiens\n" +
                          "sample2\tMus musculus\n" +
                          "sample3\tRattus norvegicus\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        assertThat(sdrfContent.getSdrfRows()).hasSize(3);
    }

    @Test
    void rowDataAccessible() throws Exception {
        String content = "source name\tcharacteristics[organism]\n" +
                          "sample1\tHomo sapiens\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        assertThat(sdrfContent.getSdrfRows().get(0).getRow()).contains("sample1", "homo sapiens");
    }

    @Test
    void nonExistentFileThrowsException() {
        SDRFParser parser = new SDRFParser();
        assertThatThrownBy(() -> parser.getSDRFContent("/nonexistent/path/sdrf.tsv"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void columnDataRetrievableByName() throws Exception {
        String content = "source name\tcharacteristics[organism]\n" +
                          "sample1\tHomo sapiens\n" +
                          "sample2\tMus musculus\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        List<String> sourceNames = sdrfContent.getColumnDataByName("source name");
        assertThat(sourceNames).containsExactly("sample1", "sample2");
    }

    @Test
    void recommendedColumnsDetectable() throws Exception {
        String content = "source name\tcharacteristics[organism]\n" +
                          "sample1\tHomo sapiens\n";

        File sdrfFile = createSdrfFile("sdrf.tsv", content);
        SDRFParser parser = new SDRFParser();
        SDRFContent sdrfContent = parser.getSDRFContent(sdrfFile.getAbsolutePath());

        List<String> columnNames = sdrfContent.getSdrfColumns().stream()
                .map(col -> col.getName().toLowerCase().trim())
                .toList();

        // These recommended columns should be missing
        assertThat(columnNames.stream().noneMatch(c -> c.contains("comment[instrument]"))).isTrue();
        assertThat(columnNames.stream().noneMatch(c -> c.contains("comment[data file]"))).isTrue();
    }

    private File createSdrfFile(String name, String content) throws IOException {
        Path filePath = tempDir.resolve(name);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        return filePath.toFile();
    }
}
