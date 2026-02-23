package uk.ac.ebi.pride.pxsubmit.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private static AppConfig config;

    @BeforeAll
    static void setUp() {
        config = AppConfig.getInstance();
    }

    @Test
    void singletonNotNull() {
        assertThat(config).isNotNull();
    }

    @Test
    void singletonSameInstance() {
        AppConfig other = AppConfig.getInstance();
        assertThat(config).isSameAs(other);
    }

    @Test
    void userLoginUrlLoaded() {
        assertThat(config.getUserLoginUrl()).isNotNull().isNotEmpty();
    }

    @Test
    void allApiUrlsUseHttps() {
        assertThat(config.getUserLoginUrl()).startsWith("https://");
        assertThat(config.getUploadDetailUrl()).startsWith("https://");
        assertThat(config.getReuploadDetailUrl()).startsWith("https://");
        assertThat(config.getUploadVerifyUrl()).startsWith("https://");
        assertThat(config.getSubmissionCompleteUrl()).startsWith("https://");
        assertThat(config.getResubmissionCompleteUrl()).startsWith("https://");
        assertThat(config.getSubmissionDetailUrl()).startsWith("https://");
        assertThat(config.getSubmissionWsBaseUrl()).startsWith("https://");
    }

    @Test
    void getUploadDetailUrlReplacesMethodPlaceholder() {
        String url = config.getUploadDetailUrl("ftp");

        assertThat(url).contains("ftp");
        assertThat(url).doesNotContain("{method}");
    }

    @Test
    void getReuploadDetailUrlReplacesBothPlaceholders() {
        String url = config.getReuploadDetailUrl("aspera", "T-123");

        assertThat(url).contains("aspera");
        assertThat(url).contains("T-123");
        assertThat(url).doesNotContain("{method}");
        assertThat(url).doesNotContain("{ticketId}");
    }

    @Test
    void getIntPropertyWithDefault() {
        int value = config.getIntProperty("nonexistent.int.prop", 42);
        assertThat(value).isEqualTo(42);
    }

    @Test
    void getIntPropertyInvalidFormat() {
        // Setting a non-numeric value should return default
        int value = config.getIntProperty("px.submission.tool.name", 99);
        assertThat(value).isEqualTo(99);
    }

    @Test
    void getBooleanPropertyWithDefault() {
        boolean value = config.getBooleanProperty("nonexistent.bool.prop", true);
        assertThat(value).isTrue();
    }

    @Test
    void proxyDisabledByDefault() {
        assertThat(config.isProxyEnabled()).isFalse();
    }

    @Test
    void toolNameNotNull() {
        assertThat(config.getToolName()).isNotNull().isNotEmpty();
    }

    @Test
    void toolVersionNotNull() {
        assertThat(config.getToolVersion()).isNotNull().isNotEmpty();
    }
}
