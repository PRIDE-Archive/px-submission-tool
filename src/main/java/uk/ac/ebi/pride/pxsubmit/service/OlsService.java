package uk.ac.ebi.pride.pxsubmit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.CvParam;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for querying EBI OLS (Ontology Lookup Service) REST API.
 * Provides autocomplete functionality for ontology terms.
 *
 * Supported ontologies:
 * - NCBITaxon: Species/organisms
 * - BTO: Tissues (BRENDA Tissue Ontology)
 * - CL: Cell types (Cell Ontology)
 * - DOID: Diseases (Disease Ontology)
 * - MS: Mass spectrometry (PSI-MS Ontology)
 * - MOD: Modifications (PSI-MOD Ontology)
 *
 * Usage:
 * <pre>
 * OlsService ols = OlsService.getInstance();
 * List<CvParam> results = ols.search("human", OlsOntology.NCBI_TAXON, 10).join();
 * </pre>
 */
public class OlsService {

    private static final Logger logger = LoggerFactory.getLogger(OlsService.class);

    // OLS API base URL
    private static final String OLS_BASE_URL = "https://www.ebi.ac.uk/ols4/api";
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String SELECT_ENDPOINT = "/select";

    // HTTP client configuration
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RESULTS = 50;

    // Singleton instance
    private static OlsService instance;

    // HTTP client and JSON mapper
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Cache for recent searches (query -> results)
    private final Map<String, List<CvParam>> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * Supported ontologies
     */
    public enum OlsOntology {
        NCBI_TAXON("ncbitaxon", "NCBITaxon", "Species/Organisms"),
        BTO("bto", "BTO", "Tissues"),
        CL("cl", "CL", "Cell Types"),
        DOID("doid", "DOID", "Diseases"),
        MS("ms", "MS", "Mass Spectrometry"),
        MOD("mod", "MOD", "Modifications"),
        EFO("efo", "EFO", "Experimental Factor"),
        PRIDE("pride", "PRIDE", "PRIDE Ontology"),
        UNIMOD("unimod", "UNIMOD", "Unimod Modifications");

        private final String olsId;
        private final String cvLabel;
        private final String displayName;

        OlsOntology(String olsId, String cvLabel, String displayName) {
            this.olsId = olsId;
            this.cvLabel = cvLabel;
            this.displayName = displayName;
        }

        public String getOlsId() {
            return olsId;
        }

        public String getCvLabel() {
            return cvLabel;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Search result with additional metadata
     */
    public record OlsSearchResult(
            CvParam cvParam,
            String description,
            String ontologyName,
            boolean isObsolete
    ) {}

    private OlsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get singleton instance
     */
    public static synchronized OlsService getInstance() {
        if (instance == null) {
            instance = new OlsService();
        }
        return instance;
    }

    /**
     * Search for terms in a specific ontology
     *
     * @param query Search query
     * @param ontology Ontology to search
     * @param maxResults Maximum number of results
     * @return CompletableFuture with list of matching CvParams
     */
    public CompletableFuture<List<CvParam>> search(String query, OlsOntology ontology, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cacheKey = ontology.olsId + ":" + query.toLowerCase().trim();

        // Check cache
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }

        String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = String.format("%s%s?q=%s&ontology=%s&rows=%d&local=true",
                OLS_BASE_URL, SELECT_ENDPOINT, encodedQuery, ontology.olsId,
                Math.min(maxResults, MAX_RESULTS));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warn("OLS search failed with status {}: {}", response.statusCode(), url);
                        return Collections.<CvParam>emptyList();
                    }
                    return parseSearchResponse(response.body(), ontology);
                })
                .thenApply(results -> {
                    // Cache results
                    if (cache.size() >= MAX_CACHE_SIZE) {
                        // Simple cache eviction - clear half
                        cache.clear();
                    }
                    cache.put(cacheKey, results);
                    return results;
                })
                .exceptionally(e -> {
                    logger.error("OLS search error for query '{}': {}", query, e.getMessage());
                    return Collections.emptyList();
                });
    }

    /**
     * Search for terms with detailed results
     */
    public CompletableFuture<List<OlsSearchResult>> searchDetailed(String query, OlsOntology ontology, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = String.format("%s%s?q=%s&ontology=%s&rows=%d&local=true",
                OLS_BASE_URL, SELECT_ENDPOINT, encodedQuery, ontology.olsId,
                Math.min(maxResults, MAX_RESULTS));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warn("OLS search failed with status {}: {}", response.statusCode(), url);
                        return Collections.<OlsSearchResult>emptyList();
                    }
                    return parseDetailedResponse(response.body(), ontology);
                })
                .exceptionally(e -> {
                    logger.error("OLS search error for query '{}': {}", query, e.getMessage());
                    return Collections.emptyList();
                });
    }

    /**
     * Search across multiple ontologies
     */
    public CompletableFuture<List<CvParam>> searchMultiple(String query, List<OlsOntology> ontologies, int maxResultsPerOntology) {
        List<CompletableFuture<List<CvParam>>> futures = ontologies.stream()
                .map(ont -> search(query, ont, maxResultsPerOntology))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .flatMap(f -> f.join().stream())
                        .toList());
    }

    /**
     * Get term by exact accession
     */
    public CompletableFuture<Optional<CvParam>> getTermByAccession(String accession, OlsOntology ontology) {
        if (accession == null || accession.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // OLS4 API uses IRI for term lookup
        String iri = URLEncoder.encode("http://purl.obolibrary.org/obo/" + accession.replace(":", "_"),
                StandardCharsets.UTF_8);
        String url = String.format("%s/ontologies/%s/terms/%s",
                OLS_BASE_URL, ontology.olsId, iri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return Optional.<CvParam>empty();
                    }
                    return parseTermResponse(response.body(), ontology);
                })
                .exceptionally(e -> {
                    logger.error("OLS term lookup error for '{}': {}", accession, e.getMessage());
                    return Optional.empty();
                });
    }

    /**
     * Parse OLS search response
     */
    private List<CvParam> parseSearchResponse(String json, OlsOntology ontology) {
        List<CvParam> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.path("response");
            JsonNode docs = response.path("docs");

            if (docs.isArray()) {
                for (JsonNode doc : docs) {
                    CvParam param = parseDoc(doc, ontology);
                    if (param != null) {
                        results.add(param);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing OLS response: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Parse detailed response
     */
    private List<OlsSearchResult> parseDetailedResponse(String json, OlsOntology ontology) {
        List<OlsSearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.path("response");
            JsonNode docs = response.path("docs");

            if (docs.isArray()) {
                for (JsonNode doc : docs) {
                    CvParam param = parseDoc(doc, ontology);
                    if (param != null) {
                        String description = doc.path("description").isArray() ?
                                doc.path("description").get(0).asText("") :
                                doc.path("description").asText("");
                        String ontName = doc.path("ontology_name").asText(ontology.getDisplayName());
                        boolean isObsolete = doc.path("is_obsolete").asBoolean(false);

                        results.add(new OlsSearchResult(param, description, ontName, isObsolete));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing OLS response: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Parse single document to CvParam
     */
    private CvParam parseDoc(JsonNode doc, OlsOntology ontology) {
        String label = doc.path("label").asText(null);
        String oboId = doc.path("obo_id").asText(null);

        if (label == null || oboId == null) {
            return null;
        }

        // Determine CV label from the accession prefix
        String cvLabel = ontology.getCvLabel();
        if (oboId.contains(":")) {
            String prefix = oboId.substring(0, oboId.indexOf(":"));
            // Map common prefixes
            cvLabel = switch (prefix.toUpperCase()) {
                case "NCBITAXON" -> "NEWT";
                case "BTO" -> "BTO";
                case "CL" -> "CL";
                case "DOID" -> "DOID";
                case "MS" -> "MS";
                case "MOD" -> "MOD";
                case "UNIMOD" -> "UNIMOD";
                default -> prefix.toUpperCase();
            };
        }

        return new CvParam(cvLabel, oboId, label, null);
    }

    /**
     * Parse term lookup response
     */
    private Optional<CvParam> parseTermResponse(String json, OlsOntology ontology) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String label = root.path("label").asText(null);
            String oboId = root.path("obo_id").asText(null);

            if (label != null && oboId != null) {
                return Optional.of(new CvParam(ontology.getCvLabel(), oboId, label, null));
            }
        } catch (Exception e) {
            logger.error("Error parsing OLS term response: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Clear the search cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get common species (for quick selection)
     */
    public static List<CvParam> getCommonSpecies() {
        return List.of(
                new CvParam("NEWT", "NCBITaxon:9606", "Homo sapiens", null),
                new CvParam("NEWT", "NCBITaxon:10090", "Mus musculus", null),
                new CvParam("NEWT", "NCBITaxon:10116", "Rattus norvegicus", null),
                new CvParam("NEWT", "NCBITaxon:9913", "Bos taurus", null),
                new CvParam("NEWT", "NCBITaxon:7955", "Danio rerio", null),
                new CvParam("NEWT", "NCBITaxon:7227", "Drosophila melanogaster", null),
                new CvParam("NEWT", "NCBITaxon:6239", "Caenorhabditis elegans", null),
                new CvParam("NEWT", "NCBITaxon:4932", "Saccharomyces cerevisiae", null),
                new CvParam("NEWT", "NCBITaxon:562", "Escherichia coli", null),
                new CvParam("NEWT", "NCBITaxon:3702", "Arabidopsis thaliana", null)
        );
    }

    /**
     * Get common instruments
     */
    public static List<CvParam> getCommonInstruments() {
        return List.of(
                new CvParam("MS", "MS:1001911", "Q Exactive", null),
                new CvParam("MS", "MS:1002877", "Q Exactive HF", null),
                new CvParam("MS", "MS:1002523", "Q Exactive Plus", null),
                new CvParam("MS", "MS:1002732", "Orbitrap Fusion", null),
                new CvParam("MS", "MS:1002416", "Orbitrap Fusion Lumos", null),
                new CvParam("MS", "MS:1003028", "Orbitrap Exploris 480", null),
                new CvParam("MS", "MS:1002996", "timsTOF Pro", null),
                new CvParam("MS", "MS:1000449", "LTQ Orbitrap", null),
                new CvParam("MS", "MS:1001910", "LTQ Orbitrap Elite", null),
                new CvParam("MS", "MS:1000658", "4800 Proteomics Analyzer", null),
                new CvParam("MS", "MS:1000190", "TripleTOF 5600", null)
        );
    }

    /**
     * Get common modifications
     */
    public static List<CvParam> getCommonModifications() {
        return List.of(
                new CvParam("MOD", "MOD:00394", "acetylated residue", null),
                new CvParam("MOD", "MOD:00696", "phosphorylated residue", null),
                new CvParam("MOD", "MOD:00719", "methylated residue", null),
                new CvParam("MOD", "MOD:01060", "S-carboxamidomethyl-L-cysteine", null),
                new CvParam("MOD", "MOD:00425", "monohydroxylated residue", null),
                new CvParam("MOD", "MOD:00412", "deamidated residue", null),
                new CvParam("UNIMOD", "UNIMOD:4", "Carbamidomethyl", null),
                new CvParam("UNIMOD", "UNIMOD:35", "Oxidation", null),
                new CvParam("UNIMOD", "UNIMOD:1", "Acetyl", null),
                new CvParam("UNIMOD", "UNIMOD:21", "Phospho", null)
        );
    }

    /**
     * Get common tissues (for quick selection)
     */
    public static List<CvParam> getCommonTissues() {
        return List.of(
                new CvParam("BTO", "BTO:0000759", "liver", null),
                new CvParam("BTO", "BTO:0000142", "brain", null),
                new CvParam("BTO", "BTO:0000562", "heart", null),
                new CvParam("BTO", "BTO:0000671", "kidney", null),
                new CvParam("BTO", "BTO:0000763", "lung", null),
                new CvParam("BTO", "BTO:0001103", "plasma", null),
                new CvParam("BTO", "BTO:0000133", "blood", null),
                new CvParam("BTO", "BTO:0001253", "serum", null),
                new CvParam("BTO", "BTO:0001078", "pancreas", null),
                new CvParam("BTO", "BTO:0000988", "muscle", null),
                new CvParam("BTO", "BTO:0001418", "urine", null),
                new CvParam("BTO", "BTO:0001175", "skin", null)
        );
    }

    /**
     * Get common diseases (for quick selection)
     */
    public static List<CvParam> getCommonDiseases() {
        return List.of(
                new CvParam("DOID", "DOID:162", "cancer", null),
                new CvParam("DOID", "DOID:684", "hepatocellular carcinoma", null),
                new CvParam("DOID", "DOID:1612", "breast cancer", null),
                new CvParam("DOID", "DOID:9256", "colorectal cancer", null),
                new CvParam("DOID", "DOID:1324", "lung cancer", null),
                new CvParam("DOID", "DOID:10283", "prostate cancer", null),
                new CvParam("DOID", "DOID:9351", "diabetes mellitus", null),
                new CvParam("DOID", "DOID:10763", "hypertension", null),
                new CvParam("DOID", "DOID:12858", "Huntington's disease", null),
                new CvParam("DOID", "DOID:10652", "Alzheimer's disease", null),
                new CvParam("DOID", "DOID:14330", "Parkinson's disease", null),
                new CvParam("MONDO", "MONDO:0005015", "healthy", null)
        );
    }

    /**
     * Get common cell types (for quick selection)
     */
    public static List<CvParam> getCommonCellTypes() {
        return List.of(
                new CvParam("CL", "CL:0000057", "fibroblast", null),
                new CvParam("CL", "CL:0000084", "T cell", null),
                new CvParam("CL", "CL:0000236", "B cell", null),
                new CvParam("CL", "CL:0000775", "neutrophil", null),
                new CvParam("CL", "CL:0000235", "macrophage", null),
                new CvParam("CL", "CL:0000182", "hepatocyte", null),
                new CvParam("CL", "CL:0002322", "embryonic stem cell", null),
                new CvParam("CL", "CL:0000034", "stem cell", null),
                new CvParam("BTO", "BTO:0000567", "HeLa cell", null),
                new CvParam("BTO", "BTO:0000017", "HEK293 cell", null),
                new CvParam("BTO", "BTO:0000944", "MCF7 cell", null),
                new CvParam("BTO", "BTO:0000093", "A549 cell", null)
        );
    }

    /**
     * Get common experiment types / MS methods (for quick selection)
     */
    public static List<CvParam> getCommonExperimentTypes() {
        return List.of(
                new CvParam("PRIDE", "PRIDE:0000428", "Bottom-up proteomics", null),
                new CvParam("PRIDE", "PRIDE:0000427", "Top-down proteomics", null),
                new CvParam("PRIDE", "PRIDE:0000627", "Data-dependent acquisition", null),
                new CvParam("PRIDE", "PRIDE:0000450", "Data-independent acquisition", null),
                new CvParam("PRIDE", "PRIDE:0000650", "diaPASEF", null),
                new CvParam("PRIDE", "PRIDE:0000447", "SWATH MS", null),
                new CvParam("PRIDE", "PRIDE:0000430", "Crosslinking MS", null),
                new CvParam("PRIDE", "PRIDE:0000433", "Affinity purification (AP-MS)", null),
                new CvParam("PRIDE", "PRIDE:0000311", "SRM/MRM", null),
                new CvParam("PRIDE", "PRIDE:0000649", "Immunopeptidomics", null),
                new CvParam("PRIDE", "PRIDE:0000648", "Metaproteomics", null),
                new CvParam("PRIDE", "PRIDE:0000647", "Proteogenomics", null),
                new CvParam("PRIDE", "PRIDE:0000665", "Glycoproteomics", null),
                new CvParam("MS", "MS:1002521", "MS imaging", null)
        );
    }

    /**
     * Get common software/tools (for quick selection)
     */
    public static List<CvParam> getCommonSoftware() {
        return List.of(
                new CvParam("MS", "MS:1001583", "MaxQuant", null),
                new CvParam("MS", "MS:1003253", "DIA-NN", null),
                new CvParam("MS", "MS:1003429", "FragPipe", null),
                new CvParam("MS", "MS:1001327", "Spectronaut", null),
                new CvParam("MS", "MS:1001207", "Mascot", null),
                new CvParam("MS", "MS:1000650", "Proteome Discoverer", null),
                new CvParam("MS", "MS:1001476", "X!Tandem", null),
                new CvParam("MS", "MS:1001585", "Andromeda", null),
                new CvParam("MS", "MS:1001456", "Skyline", null),
                new CvParam("MS", "MS:1002076", "MSFragger", null),
                new CvParam("MS", "MS:1002038", "Proteome Discoverer Sequest HT", null),
                new CvParam("MS", "MS:1001475", "OMSSA", null)
        );
    }
}
