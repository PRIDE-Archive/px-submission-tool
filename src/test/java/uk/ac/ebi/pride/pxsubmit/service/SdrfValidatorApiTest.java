package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SdrfValidatorApiTest {

    @Test
    void buildValidateUriIncludesAllQueryParameters() {
        SdrfValidationOptions options = new SdrfValidationOptions(
                List.of("ms-proteomics", "human"),
                true,
                false
        );

        String uri = SdrfValidatorApi.buildValidateUri(options);

        assertThat(uri).startsWith(SdrfValidatorApi.VALIDATE_URL);
        assertThat(uri).contains("skip_ontology=true");
        assertThat(uri).contains("use_ols_cache_only=false");
        assertThat(uri).contains("template=ms-proteomics");
        assertThat(uri).contains("template=human");
    }

    @Test
    void buildValidateUriUsesApiDefaults() {
        String uri = SdrfValidatorApi.buildValidateUri(
                SdrfValidationOptions.defaults(List.of("default")));

        assertThat(uri).contains("skip_ontology=false");
        assertThat(uri).contains("use_ols_cache_only=true");
        assertThat(uri).contains("template=default");
    }

    @Test
    void buildValidateUriEncodesTemplateNames() {
        String uri = SdrfValidatorApi.buildValidateUri(
                new SdrfValidationOptions(List.of("dia-acquisition"), false, true));

        assertThat(uri).contains("template=dia-acquisition");
    }

    @Test
    void buildValidateUriRejectsEmptyTemplates() {
        assertThatThrownBy(() -> SdrfValidatorApi.buildValidateUri(
                new SdrfValidationOptions(List.of(), false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template");
    }
}
