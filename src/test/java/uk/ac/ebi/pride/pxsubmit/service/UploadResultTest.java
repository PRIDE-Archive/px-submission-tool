package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadResultTest {

    @Test
    void ftpResultSuccess() {
        UploadResult result = UploadResult.ftpResult(true, 5, 0, 1024L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(5);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getBytesUploaded()).isEqualTo(1024L);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void ftpResultFailure() {
        UploadResult result = UploadResult.ftpResult(false, 3, 2, 500L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(2);
    }

    @Test
    void ftpResultZeroCounts() {
        UploadResult result = UploadResult.ftpResult(true, 0, 0, 0L);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getBytesUploaded()).isEqualTo(0L);
    }

    @Test
    void ftpResultBytesUploadedStoredCorrectly() {
        long largeBytes = 10_000_000_000L;
        UploadResult result = UploadResult.ftpResult(true, 1, 0, largeBytes);

        assertThat(result.getBytesUploaded()).isEqualTo(largeBytes);
    }

    @Test
    void asperaResultSuccess() {
        UploadResult result = UploadResult.asperaResult(true, 10, 0, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(10);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getBytesUploaded()).isEqualTo(0L);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void asperaResultFailureWithErrorMessage() {
        UploadResult result = UploadResult.asperaResult(false, 0, 5, "Connection refused");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");
    }

    @Test
    void asperaResultNullErrorMessage() {
        UploadResult result = UploadResult.asperaResult(true, 1, 0, null);

        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void asperaResultBytesUploadedAlwaysZero() {
        UploadResult result = UploadResult.asperaResult(true, 5, 0, null);

        assertThat(result.getBytesUploaded()).isEqualTo(0L);
    }
}
