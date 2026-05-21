package uk.ac.ebi.pride.pxsubmit.service;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.submissions.commons.dtos.CompressionConfigDTO;
import uk.ac.ebi.pride.submissions.commons.dtos.FileTransferEntryDTO;
import uk.ac.ebi.pride.submissions.commons.dtos.RequestsQueueItemDTO;
import uk.ac.ebi.pride.submissions.commons.services.SubmissionQueuedRequestsService;

import java.io.File;
import java.io.IOException;
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

            entries.add(new FileTransferEntryDTO(
                    file.getName(),
                    true,
                    false,
                    false,
                    file.length(),
                    file.getAbsolutePath()
            ));
        }
        return entries;
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
