package uk.ac.ebi.pride.pxsubmit.service;

import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.submissions.commons.dtos.CompressionConfigDTO;
import uk.ac.ebi.pride.submissions.commons.dtos.FileTransferEntryDTO;
import uk.ac.ebi.pride.submissions.commons.dtos.RequestsQueueItemDTO;
import uk.ac.ebi.pride.submissions.commons.services.SubmissionQueuedRequestsService;
import uk.ac.ebi.pride.submissions.commons.types.FileCategory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Queued-requests service that lists files from the submission tool's {@link DataFile} selection
 * (multiple folders), instead of scanning a single directory path.
 * <p>
 * Compatible with {@code pride-submissions-commons} 1.0.6+ ({@link FileTransferEntryDTO} fields,
 * {@link #readFile(FileTransferEntryDTO)}).
 */
public class ModelFilesQueuedRequestsService implements SubmissionQueuedRequestsService {

    private final Map<String, RequestsQueueItemDTO> requests = new HashMap<>();
    private final List<DataFile> dataFiles;

    public ModelFilesQueuedRequestsService(List<DataFile> dataFiles) {
        this.dataFiles = List.copyOf(dataFiles);
    }

    @Override
    public void queueRequest(String ticketId, RequestsQueueItemDTO item, boolean replace) {
        requests.put(ticketId, item);
    }

    @Override
    public Optional<RequestsQueueItemDTO> getRequest(String ticketId) {
        return Optional.ofNullable(requests.get(ticketId));
    }

    @Override
    public List<FileTransferEntryDTO> listDirectory(CompressionConfigDTO compression, boolean performFileReads, Object... params)
            throws IOException {
        Set<String> rejected = new HashSet<>();
        if (compression != null && compression.getRejectedFormats() != null) {
            rejected.addAll(compression.getRejectedFormats());
        }

        List<FileTransferEntryDTO> entries = new ArrayList<>();
        for (DataFile dataFile : dataFiles) {
            File file = dataFile.getFile();
            if (file == null || !file.isFile()) {
                continue;
            }

            String extension = extensionOf(file.getName());
            if (!extension.isEmpty() && rejected.contains(extension)) {
                throw new IOException("Rejected compressed format '." + extension + "' for file: " + file.getName());
            }

            File parent = file.getParentFile();
            entries.add(new FileTransferEntryDTO(
                    file.getName(),
                    parent != null ? parent.getName() : "",
                    true,
                    false,
                    false,
                    0,
                    file.length(),
                    file.getAbsolutePath(),
                    toFileCategory(dataFile)
            ));
        }
        return entries;
    }

    @Override
    public InputStream readFile(FileTransferEntryDTO entry) throws IOException {
        if (entry == null || entry.getPath() == null || entry.getPath().isBlank()) {
            throw new IOException("Missing file path for: " + (entry != null ? entry.getName() : "unknown"));
        }
        File file = new File(entry.getPath());
        if (!file.isFile()) {
            throw new IOException("Cannot read file: " + entry.getPath());
        }
        return new FileInputStream(file);
    }

    private static FileCategory toFileCategory(DataFile dataFile) {
        ProjectFileType type = dataFile.getFileType();
        if (type == null) {
            return FileCategory.OTHER;
        }
        return switch (type) {
            case RAW -> FileCategory.RAW;
            case PEAK -> FileCategory.PEAK;
            case SEARCH, RESULT -> FileCategory.ANALYSIS;
            case EXPERIMENTAL_DESIGN -> FileCategory.METADATA;
            default -> FileCategory.OTHER;
        };
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
