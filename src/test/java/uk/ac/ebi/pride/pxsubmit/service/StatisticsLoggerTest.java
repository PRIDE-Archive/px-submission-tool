package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics.FileTransferStat;

import static org.assertj.core.api.Assertions.assertThatNoException;

class StatisticsLoggerTest {

    @Test
    void logSessionStartDoesNotThrow() {
        TransferStatistics stats = new TransferStatistics("FTP", 2, 1000L);

        assertThatNoException().isThrownBy(() -> StatisticsLogger.logSessionStart(stats));
    }

    @Test
    void logFileStartDoesNotThrow() {
        TransferStatistics stats = new TransferStatistics("FTP", 1, 500L);
        FileTransferStat fileStat = stats.startFile("test.raw", 500L);

        assertThatNoException().isThrownBy(() -> StatisticsLogger.logFileStart(fileStat));
    }

    @Test
    void logFileCompleteDoesNotThrow() {
        TransferStatistics stats = new TransferStatistics("FTP", 1, 500L);
        FileTransferStat fileStat = stats.startFile("test.raw", 500L);
        fileStat.complete(true, null, 500L);

        assertThatNoException().isThrownBy(() -> StatisticsLogger.logFileComplete(fileStat));
    }

    @Test
    void logSessionSummaryDoesNotThrow() {
        TransferStatistics stats = new TransferStatistics("FTP", 1, 500L);
        stats.startFile("test.raw", 500L);
        stats.completeFile("test.raw", 500L);
        stats.setSessionComplete();

        assertThatNoException().isThrownBy(() -> StatisticsLogger.logSessionSummary(stats));
    }

    @Test
    void logEventDoesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                StatisticsLogger.logEvent("session-1", "RETRY", "Connection timeout"));
    }

    @Test
    void logConnectionAttemptDoesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                StatisticsLogger.logConnectionAttempt("session-1", "ftp.example.com", 21, true, 150L));
    }
}
