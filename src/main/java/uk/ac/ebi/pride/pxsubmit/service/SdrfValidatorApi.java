package uk.ac.ebi.pride.pxsubmit.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Helpers for the PRIDE SDRF Validator REST API.
 */
public final class SdrfValidatorApi {

    public static final String VALIDATE_URL = SdrfValidationOptions.DEFAULT_VALIDATE_URL;
    public static final String TEMPLATES_URL =
            "https://www.ebi.ac.uk/pride/services/sdrf-validator/templates";

    private SdrfValidatorApi() {
    }

    /**
     * Builds the validate endpoint URI with all supported query parameters.
     */
    public static String buildValidateUri(SdrfValidationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        List<String> templates = options.templates();
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("At least one template is required");
        }

        StringBuilder uri = new StringBuilder(VALIDATE_URL);
        uri.append("?skip_ontology=").append(options.skipOntology());
        uri.append("&use_ols_cache_only=").append(options.useOlsCacheOnly());
        for (String template : templates) {
            uri.append("&template=")
                    .append(URLEncoder.encode(template, StandardCharsets.UTF_8));
        }
        return uri.toString();
    }
}
