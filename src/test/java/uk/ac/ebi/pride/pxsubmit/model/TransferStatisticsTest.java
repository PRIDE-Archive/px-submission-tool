package uk.ac.ebi.pride.pxsubmit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics.FileTransferStat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransferStatistics and FileTransferStat classes.
 */
class TransferStatisticsTest {

    private TransferStatistics stats;

    @BeforeEach
    void setUp() {
        stats = new TransferStatistics("FTP", 3, 1000000L);
    }

    @Test
    void testSessionInitialization() {
        assertNotNull(stats.getSessionId());
        assertEquals(8, stats.getSessionId().length());
        assertEquals("FTP", stats.getUploadMethod());
        assertEquals(3, stats.getTotalFiles());
        assertEquals(1000000L, stats.getTotalBytes());
        assertNotNull(stats.getStartTime());
        assertNull(stats.getEndTime());
        assertEquals(0, stats.getFilesCompleted());
        assertEquals(0, stats.getFilesFailed());
        assertEquals(0L, stats.getBytesTransferred());
    }

    @Test
    void testStartFile() {
        FileTransferStat fileStat = stats.startFile("test.txt", 500L);

        assertNotNull(fileStat);
        assertEquals("test.txt", fileStat.getFileName());
        assertEquals(500L, fileStat.getFileSize());
        assertEquals(stats.getSessionId(), fileStat.getSessionId());
        assertNotNull(fileStat.getStartTime());
        assertNull(fileStat.getEndTime());
        assertFalse(fileStat.isSuccess());
        assertEquals(0, fileStat.getRetries());
    }

    @Test
    void testCompleteFile() {
        stats.startFile("test.txt", 500L);
        stats.completeFile("test.txt", 500L);

        assertEquals(1, stats.getFilesCompleted());
        assertEquals(0, stats.getFilesFailed());
        assertEquals(500L, stats.getBytesTransferred());

        FileTransferStat fileStat = stats.getFileStat("test.txt");
        assertNotNull(fileStat);
        assertTrue(fileStat.isSuccess());
        assertNull(fileStat.getError());
        assertEquals(500L, fileStat.getBytesTransferred());
    }

    @Test
    void testFailFile() {
        stats.startFile("test.txt", 500L);
        stats.failFile("test.txt", "Connection timeout", 2);

        assertEquals(0, stats.getFilesCompleted());
        assertEquals(1, stats.getFilesFailed());
        assertEquals(0L, stats.getBytesTransferred());

        FileTransferStat fileStat = stats.getFileStat("test.txt");
        assertNotNull(fileStat);
        assertFalse(fileStat.isSuccess());
        assertEquals("Connection timeout", fileStat.getError());
        assertEquals(2, fileStat.getRetries());
    }

    @Test
    void testFailFileNotStarted() {
        // Fail a file that was never started
        stats.failFile("unknown.txt", "File not found", 0);

        assertEquals(0, stats.getFilesCompleted());
        assertEquals(1, stats.getFilesFailed());
    }

    @Test
    void testSessionComplete() {
        stats.startFile("file1.txt", 300L);
        stats.startFile("file2.txt", 400L);
        stats.startFile("file3.txt", 300L);

        stats.completeFile("file1.txt", 300L);
        stats.completeFile("file2.txt", 400L);
        stats.completeFile("file3.txt", 300L);

        stats.setSessionComplete();

        assertTrue(stats.isComplete());
        assertTrue(stats.isSuccessful());
        assertNotNull(stats.getEndTime());
        assertEquals(3, stats.getFilesCompleted());
        assertEquals(0, stats.getFilesFailed());
        assertEquals(1000L, stats.getBytesTransferred());
    }

    @Test
    void testSessionWithFailures() {
        stats.startFile("file1.txt", 300L);
        stats.startFile("file2.txt", 400L);
        stats.startFile("file3.txt", 300L);

        stats.completeFile("file1.txt", 300L);
        stats.failFile("file2.txt", "Error", 1);
        stats.completeFile("file3.txt", 300L);

        stats.setSessionComplete();

        assertTrue(stats.isComplete());
        assertFalse(stats.isSuccessful());
        assertEquals(2, stats.getFilesCompleted());
        assertEquals(1, stats.getFilesFailed());
    }

    @Test
    void testTotalDuration() {
        // Duration should be positive even without session complete
        assertTrue(stats.getTotalDurationMs() >= 0);

        stats.setSessionComplete();

        // Duration should still be positive after completion
        assertTrue(stats.getTotalDurationMs() >= 0);
    }

    @Test
    void testAverageRateWithNoData() {
        assertEquals(0.0, stats.getAverageRateMBps(), 0.001);
    }

    @Test
    void testCompletedFileStats() {
        stats.startFile("file1.txt", 300L);
        stats.startFile("file2.txt", 400L);

        stats.completeFile("file1.txt", 300L);
        stats.failFile("file2.txt", "Error", 0);

        assertEquals(2, stats.getCompletedFileStats().size());
    }

    @Test
    void testToString() {
        String str = stats.toString();
        assertTrue(str.contains("TransferStatistics"));
        assertTrue(str.contains("FTP"));
        assertTrue(str.contains(stats.getSessionId()));
    }

    // FileTransferStat specific tests

    @Test
    void testFileTransferStatIncrementRetries() {
        FileTransferStat fileStat = stats.startFile("test.txt", 500L);

        assertEquals(0, fileStat.getRetries());
        fileStat.incrementRetries();
        assertEquals(1, fileStat.getRetries());
        fileStat.incrementRetries();
        assertEquals(2, fileStat.getRetries());
    }

    @Test
    void testFileTransferStatDuration() {
        FileTransferStat fileStat = stats.startFile("test.txt", 500L);

        // Duration should be non-negative
        assertTrue(fileStat.getDurationMs() >= 0);

        fileStat.complete(true, null, 500L);

        // Duration should still be non-negative after completion
        assertTrue(fileStat.getDurationMs() >= 0);
    }

    @Test
    void testFileTransferStatRateWithNoData() {
        FileTransferStat fileStat = stats.startFile("test.txt", 0L);
        fileStat.complete(true, null, 0L);

        assertEquals(0.0, fileStat.getRateMBps(), 0.001);
    }

    @Test
    void testFileTransferStatToString() {
        FileTransferStat fileStat = stats.startFile("test.txt", 500L);
        fileStat.complete(true, null, 500L);

        String str = fileStat.toString();
        assertTrue(str.contains("FileTransferStat"));
        assertTrue(str.contains("test.txt"));
        assertTrue(str.contains("500"));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        TransferStatistics concurrentStats = new TransferStatistics("FTP", 100, 100000L);

        // Start multiple files concurrently
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    String fileName = "file_" + index + "_" + j + ".txt";
                    concurrentStats.startFile(fileName, 100L);
                    concurrentStats.completeFile(fileName, 100L);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(100, concurrentStats.getFilesCompleted());
        assertEquals(10000L, concurrentStats.getBytesTransferred());
    }
}
