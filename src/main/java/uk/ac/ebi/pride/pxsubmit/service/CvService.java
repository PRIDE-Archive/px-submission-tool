package uk.ac.ebi.pride.pxsubmit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.CvParam;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading CV (controlled vocabulary) files from GitHub.
 * Falls back to local bundled CV files if GitHub is unreachable.
 *
 * CV files are loaded from:
 * https://github.com/PRIDE-Archive/pride-ontology/tree/master/submission-tool
 */
public class CvService {

    private static final Logger logger = LoggerFactory.getLogger(CvService.class);

    private static final String GITHUB_RAW_BASE_URL =
            "https://raw.githubusercontent.com/PRIDE-Archive/pride-ontology/master/submission-tool/";

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static CvService instance;

    private final HttpClient httpClient;
    private final Map<String, List<CvParam>> cvParamCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> stringCache = new ConcurrentHashMap<>();

    private CvService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    public static synchronized CvService getInstance() {
        if (instance == null) {
            instance = new CvService();
        }
        return instance;
    }

    /**
     * Load a CV file as CvParam list (for tab-separated files with 4 columns: prefix, accession, name, displayName).
     * Fetches from GitHub first, falls back to local resource.
     */
    public CompletableFuture<List<CvParam>> loadCvParams(String cvFileName) {
        List<CvParam> cached = cvParamCache.get(cvFileName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return fetchFromGitHub(cvFileName).thenApply(content -> {
            List<CvParam> params = parseCvParams(content);
            if (!params.isEmpty()) {
                cvParamCache.put(cvFileName, params);
            }
            return params;
        }).exceptionally(ex -> {
            logger.warn("Failed to fetch {} from GitHub, falling back to local", cvFileName, ex);
            List<CvParam> local = loadCvParamsLocal(cvFileName);
            if (!local.isEmpty()) {
                cvParamCache.put(cvFileName, local);
            }
            return local;
        });
    }

    /**
     * Load a CV file as a simple string list (one entry per line, e.g., projecttag.cv).
     * Fetches from GitHub first, falls back to local resource.
     */
    public CompletableFuture<List<String>> loadStringList(String cvFileName) {
        List<String> cached = stringCache.get(cvFileName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return fetchFromGitHub(cvFileName).thenApply(content -> {
            List<String> lines = parseStringList(content);
            if (!lines.isEmpty()) {
                stringCache.put(cvFileName, lines);
            }
            return lines;
        }).exceptionally(ex -> {
            logger.warn("Failed to fetch {} from GitHub, falling back to local", cvFileName, ex);
            List<String> local = loadStringListLocal(cvFileName);
            if (!local.isEmpty()) {
                stringCache.put(cvFileName, local);
            }
            return local;
        });
    }

    private CompletableFuture<String> fetchFromGitHub(String cvFileName) {
        String url = GITHUB_RAW_BASE_URL + cvFileName;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        logger.info("Fetching CV file from GitHub: {}", url);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        logger.info("Successfully loaded {} from GitHub", cvFileName);
                        return response.body();
                    }
                    throw new RuntimeException("HTTP " + response.statusCode() + " fetching " + cvFileName);
                });
    }

    private List<CvParam> parseCvParams(String content) {
        List<CvParam> params = new ArrayList<>();
        for (String line : content.split("\\R")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t");
            if (parts.length >= 3) {
                String cvLabel = parts[0].trim();
                String accession = parts[1].trim();
                String name = parts[2].trim();
                params.add(new CvParam(cvLabel, accession, name, null));
            }
        }
        return params;
    }

    private List<String> parseStringList(String content) {
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\\R")) {
            line = line.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<CvParam> loadCvParamsLocal(String cvFileName) {
        List<CvParam> params = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/cv/" + cvFileName)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        String[] parts = line.split("\t");
                        if (parts.length >= 3) {
                            String cvLabel = parts[0].trim();
                            String accession = parts[1].trim();
                            String name = parts[2].trim();
                            params.add(new CvParam(cvLabel, accession, name, null));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load local CV file: {}", cvFileName, e);
        }
        return params;
    }

    private List<String> loadStringListLocal(String cvFileName) {
        List<String> lines = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/cv/" + cvFileName)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            lines.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load local CV file: {}", cvFileName, e);
        }
        return lines;
    }

    /**
     * Clear all caches (useful for testing or forcing a refresh).
     */
    public void clearCache() {
        cvParamCache.clear();
        stringCache.clear();
    }
}
