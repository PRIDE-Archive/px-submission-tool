package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.pxsubmit.service.OlsService.OlsOntology;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OlsServiceTest {

    @Test
    void getCommonSpeciesNotEmpty() {
        assertThat(OlsService.getCommonSpecies()).isNotEmpty();
    }

    @Test
    void getCommonInstrumentsNotEmpty() {
        assertThat(OlsService.getCommonInstruments()).isNotEmpty();
    }

    @Test
    void getCommonModificationsNotEmpty() {
        assertThat(OlsService.getCommonModifications()).isNotEmpty();
    }

    @Test
    void getCommonTissuesNotEmpty() {
        assertThat(OlsService.getCommonTissues()).isNotEmpty();
    }

    @Test
    void getCommonDiseasesNotEmpty() {
        assertThat(OlsService.getCommonDiseases()).isNotEmpty();
    }

    @Test
    void getCommonCellTypesNotEmpty() {
        assertThat(OlsService.getCommonCellTypes()).isNotEmpty();
    }

    @Test
    void getCommonExperimentTypesNotEmpty() {
        assertThat(OlsService.getCommonExperimentTypes()).isNotEmpty();
    }

    @Test
    void getCommonSoftwareNotEmpty() {
        assertThat(OlsService.getCommonSoftware()).isNotEmpty();
    }

    @Test
    void getCommonSpeciesContainsHomoSapiens() {
        List<CvParam> species = OlsService.getCommonSpecies();
        boolean found = species.stream()
                .anyMatch(cv -> "NCBITaxon:9606".equals(cv.getAccession())
                        && "Homo sapiens".equals(cv.getName()));

        assertThat(found).isTrue();
    }

    @Test
    void olsOntologyValuesExist() {
        OlsOntology[] values = OlsOntology.values();
        assertThat(values.length).isGreaterThan(0);
    }

    @Test
    void olsOntologyNcbiTaxonOlsId() {
        assertThat(OlsOntology.NCBI_TAXON.getOlsId()).isEqualTo("ncbitaxon");
    }

    @Test
    void olsOntologyMsCvLabel() {
        assertThat(OlsOntology.MS.getCvLabel()).isEqualTo("MS");
    }
}
