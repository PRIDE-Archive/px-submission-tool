package uk.ac.ebi.pride.gui.aspera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uploads files using the bundled ascp 4.x CLI (no FaspManager Java SDK).
 */
public class AscpCliUploader {

    private static final Logger logger = LoggerFactory.getLogger(AscpCliUploader.class);
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,3})%");
    private static final ExecutorService TRANSFER_EXECUTOR =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "ascp-transfer");
                t.setDaemon(true);
                return t;
            });

    private final File ascpExecutable;
    private final AscpTransferConfig config;
    private AscpTransferListener listener;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Process activeProcess;

    public AscpCliUploader(File ascpExecutable, AscpTransferConfig config) {
        if (ascpExecutable == null || !ascpExecutable.exists()) {
            throw new IllegalArgumentException("Specified ascp executable does not exist: " + ascpExecutable);
        }
        this.ascpExecutable = ascpExecutable;
        this.config = config != null ? config : AscpTransferConfig.fromAppContext();
        this.listener = new DefaultAscpTransferListener();
    }

    public void setListener(AscpTransferListener listener) {
        this.listener = listener != null ? listener : new DefaultAscpTransferListener();
    }

    /**
     * Starts an upload asynchronously and returns a session id (process id string).
     */
    public String startTransfer(
            Collection<File> filesToUpload,
            String host,
            String user,
            String password,
            String remoteDirectory)
            throws AscpTransferException {
        if (filesToUpload == null || filesToUpload.isEmpty()) {
            throw new AscpTransferException("No files to upload");
        }
        if (host == null || host.isBlank() || user == null || user.isBlank()) {
            throw new AscpTransferException("Host and username are required for ascp upload");
        }

        List<String> command = buildCommand(filesToUpload, host, user, remoteDirectory);
        logger.info("Starting ascp CLI transfer: {}", maskPassword(command));

        String sessionId = "ascp-" + System.currentTimeMillis();
        int totalFiles = filesToUpload.size();
        long totalBytes = filesToUpload.stream().mapToLong(File::length).sum();

        TRANSFER_EXECUTOR.submit(() ->
                runTransfer(sessionId, command, password, totalFiles, totalBytes, filesToUpload));

        return sessionId;
    }

    public void cancel() {
        cancelled.set(true);
        Process process = activeProcess;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    List<String> buildCommand(
            Collection<File> filesToUpload, String host, String user, String remoteDirectory) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ascpExecutable.getAbsolutePath());
        cmd.add("-v");
        cmd.add("-P");
        cmd.add(String.valueOf(config.getTcpPort()));
        cmd.add("-O");
        cmd.add(String.valueOf(config.getUdpPort()));
        cmd.add("-l");
        cmd.add(formatRate(config.getTargetRateKbps()));
        cmd.add("-m");
        cmd.add(formatRate(config.getMinimumRateKbps()));
        cmd.add("--policy=" + config.getPolicy());
        cmd.add("--overwrite=" + config.getOverwrite());
        cmd.add("-k");
        cmd.add(String.valueOf(config.getResumeLevel()));
        if (config.isPreserveTimestamps()) {
            cmd.add("-p");
        }
        if (config.isCreatePath()) {
            cmd.add("-d");
        }
        if (config.getToken() != null && !config.getToken().isBlank()) {
            cmd.add("-W");
            cmd.add(config.getToken().trim());
        }

        for (File file : filesToUpload) {
            cmd.add(file.getAbsolutePath());
        }

        String remotePath = normalizeRemotePath(remoteDirectory);
        cmd.add(user + "@" + host + ":" + remotePath);
        return cmd;
    }

    private void runTransfer(
            String sessionId,
            List<String> command,
            String password,
            int totalFiles,
            long totalBytes,
            Collection<File> filesToUpload) {
        AscpTransferManager.getInstance().transferStarted();
        long startNanos = System.nanoTime();
        int filesComplete = 0;
        long transferredBytes = 0;

        try {
            notifyListener(
                    AscpTransferEvent.CONNECTING,
                    sessionStats(sessionId, 0, 0, startNanos, AscpSessionState.RUNNING, totalFiles, totalBytes),
                    null);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (password != null && !password.isBlank()) {
                pb.environment().put("ASPERA_SCP_PASS", password);
            }

            Process process = pb.start();
            activeProcess = process;

            notifyListener(
                    AscpTransferEvent.SESSION_START,
                    sessionStats(sessionId, 0, 0, startNanos, AscpSessionState.RUNNING, totalFiles, totalBytes),
                    null);

            String lastFile = null;
            int lastPercent = -1;

            try (InputStream in = process.getInputStream();
                 BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.get()) {
                        process.destroyForcibly();
                        break;
                    }
                    logger.debug("ascp> {}", line);

                    Matcher matcher = PERCENT_PATTERN.matcher(line);
                    if (matcher.find()) {
                        int percent = Integer.parseInt(matcher.group(1));
                        String fileName = extractFileName(line, filesToUpload);
                        if (fileName == null) {
                            fileName = lastFile;
                        } else {
                            lastFile = fileName;
                        }

                        if (percent != lastPercent) {
                            lastPercent = percent;
                            AscpFileInfo fileInfo =
                                    new AscpFileInfo(
                                            fileName != null ? fileName : "",
                                            percent >= 100 ? AscpFileState.FINISHED : AscpFileState.IN_PROGRESS,
                                            0,
                                            0);
                            notifyListener(
                                    AscpTransferEvent.PROGRESS,
                                    sessionStats(
                                            sessionId,
                                            filesComplete,
                                            transferredBytes,
                                            startNanos,
                                            AscpSessionState.RUNNING,
                                            totalFiles,
                                            totalBytes),
                                    fileInfo);
                        }

                        if (percent >= 100 && fileName != null) {
                            File matched = findFile(filesToUpload, fileName);
                            if (matched != null) {
                                transferredBytes += matched.length();
                            }
                            filesComplete++;
                            lastPercent = -1;
                            notifyListener(
                                    AscpTransferEvent.FILE_STOP,
                                    sessionStats(
                                            sessionId,
                                            filesComplete,
                                            transferredBytes,
                                            startNanos,
                                            AscpSessionState.RUNNING,
                                            totalFiles,
                                            totalBytes),
                                    new AscpFileInfo(fileName, AscpFileState.FINISHED, 0, 0));
                        }
                    } else if (line.toLowerCase().contains("error") || line.toLowerCase().contains("failed")) {
                        AscpTransferEvent err = AscpTransferEvent.FILE_ERROR;
                        err.setDescription(line);
                        notifyListener(
                                err,
                                sessionStats(
                                        sessionId,
                                        filesComplete,
                                        transferredBytes,
                                        startNanos,
                                        AscpSessionState.FAILED,
                                        totalFiles,
                                        totalBytes),
                                new AscpFileInfo(lastFile != null ? lastFile : "", AscpFileState.FAILED, 0, 0, line, 1));
                    }
                }
            }

            int exitCode = process.waitFor();
            activeProcess = null;

            if (cancelled.get()) {
                logger.info("ascp transfer cancelled");
                return;
            }

            if (exitCode == 0) {
                filesComplete = totalFiles;
                transferredBytes = totalBytes;
                notifyListener(
                        AscpTransferEvent.SESSION_STOP,
                        sessionStats(
                                sessionId,
                                filesComplete,
                                transferredBytes,
                                startNanos,
                                AscpSessionState.COMPLETED,
                                totalFiles,
                                totalBytes),
                        null);
            } else {
                AscpTransferEvent err = AscpTransferEvent.SESSION_ERROR;
                err.setDescription("ascp exited with code " + exitCode);
                notifyListener(
                        err,
                        sessionStats(
                                sessionId,
                                filesComplete,
                                transferredBytes,
                                startNanos,
                                AscpSessionState.FAILED,
                                totalFiles,
                                totalBytes,
                                "ascp exited with code " + exitCode,
                                exitCode),
                        null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AscpTransferEvent err = AscpTransferEvent.SESSION_ERROR;
            err.setDescription(e.getMessage());
            notifyListener(
                    err,
                    sessionStats(
                            sessionId,
                            filesComplete,
                            transferredBytes,
                            startNanos,
                            AscpSessionState.FAILED,
                            totalFiles,
                            totalBytes,
                            e.getMessage(),
                            -1),
                    null);
        } catch (IOException e) {
            AscpTransferEvent err = AscpTransferEvent.SESSION_ERROR;
            err.setDescription(e.getMessage());
            notifyListener(
                    err,
                    sessionStats(
                            sessionId,
                            filesComplete,
                            transferredBytes,
                            startNanos,
                            AscpSessionState.FAILED,
                            totalFiles,
                            totalBytes,
                            e.getMessage(),
                            -1),
                    null);
        } finally {
            AscpTransferManager.getInstance().transferFinished();
        }
    }

    private AscpSessionStats sessionStats(
            String sessionId,
            long filesComplete,
            long transferredBytes,
            long startNanos,
            AscpSessionState state,
            int totalFiles,
            long totalBytes) {
        return sessionStats(
                sessionId, filesComplete, transferredBytes, startNanos, state, totalFiles, totalBytes, null, 0);
    }

    private AscpSessionStats sessionStats(
            String sessionId,
            long filesComplete,
            long transferredBytes,
            long startNanos,
            AscpSessionState state,
            int totalFiles,
            long totalBytes,
            String errorDescription,
            int errorCode) {
        long elapsedUSec = (System.nanoTime() - startNanos) / 1000;
        return new AscpSessionStats(
                sessionId,
                filesComplete,
                transferredBytes,
                config.getTargetRateKbps(),
                config.getMinimumRateKbps(),
                elapsedUSec,
                state,
                errorDescription,
                errorCode);
    }

    private void notifyListener(AscpTransferEvent event, AscpSessionStats stats, AscpFileInfo fileInfo) {
        try {
            listener.fileSessionEvent(event, stats, fileInfo);
        } catch (Exception e) {
            logger.warn("Transfer listener failed for event {}", event, e);
        }
    }

    static String formatRate(int kbps) {
        if (kbps >= 1_000_000) {
            return (kbps / 1_000_000) + "G";
        }
        if (kbps >= 1000) {
            return (kbps / 1000) + "M";
        }
        return kbps + "K";
    }

    static String normalizeRemotePath(String remoteDirectory) {
        if (remoteDirectory == null || remoteDirectory.isBlank()) {
            return "/";
        }
        String path = remoteDirectory.trim().replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }

    private static String extractFileName(String line, Collection<File> files) {
        for (File file : files) {
            String name = file.getName();
            if (line.contains(name)) {
                return name;
            }
        }
        return null;
    }

    private static File findFile(Collection<File> files, String name) {
        for (File file : files) {
            if (file.getName().equals(name) || file.getAbsolutePath().endsWith(name)) {
                return file;
            }
        }
        return null;
    }

    private static String maskPassword(List<String> command) {
        return String.join(" ", command);
    }
}
