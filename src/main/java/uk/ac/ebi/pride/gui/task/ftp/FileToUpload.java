package uk.ac.ebi.pride.gui.task.ftp;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * FileToUpload represents a file to be uploaded
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileToUpload implements Serializable {
    private File file;
    private String folder;
    private String fileName;

    public FileToUpload(File file,
                        String folder,
                        String fileName) {
        this.file = file;
        this.folder = folder;
        this.fileName = fileName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileToUpload that = (FileToUpload) o;

        if (!Objects.equals(file, that.file)) return false;
        if (!Objects.equals(fileName, that.fileName)) return false;
        return Objects.equals(folder, that.folder);
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileToUpload{" +
                "file=" + file +
                ", folder='" + folder + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
