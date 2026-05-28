package uk.ac.ebi.pride.pxsubmit.service;

import java.util.List;

/**
 * Query parameters for the PRIDE SDRF Validator {@code POST /validate} endpoint.
 *
 * @see <a href="https://www.ebi.ac.uk/pride/services/sdrf-validator/docs#/Validation/validate_sdrf_validate_post">SDRF Validator API</a>
 */
public record SdrfValidationOptions(
        List<String> templates,
        boolean skipOntology,
        boolean useOlsCacheOnly
) {
    public static final String DEFAULT_VALIDATE_URL =
            "https://www.ebi.ac.uk/pride/services/sdrf-validator/validate";

    /** API defaults: ontology validation on, OLS cache only. */
    public static SdrfValidationOptions defaults(List<String> templates) {
        return new SdrfValidationOptions(templates, false, true);
    }
}
