package uk.ac.ebi.pride.gui.task.ftp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Ftp details for connecting the ftp server
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FTPDetail implements Serializable {
    private String ftpAddress;
    private int port;
    private String folder;
    private String userName;
    private char[] password;

    public FTPDetail(String ftpAddress,
                     int port,
                     String folder,
                     String userName,
                     char[] password) {
        this.ftpAddress = ftpAddress;
        this.port = port;
        this.folder = folder;
        this.userName = userName;
        this.password = password;
    }

    public String getFtpAddress() {
        return ftpAddress;
    }

    public int getPort() {
        return port;
    }

    public String getFolder() {
        return folder;
    }

    public String getUserName() {
        return userName;
    }

    public char[] getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FTPDetail)) return false;

        FTPDetail ftpDetail = (FTPDetail) o;

        if (port != ftpDetail.port) return false;
        if (!Objects.equals(folder, ftpDetail.folder)) return false;
        if (!Objects.equals(ftpAddress, ftpDetail.ftpAddress)) return false;
        if (!Arrays.equals(password, ftpDetail.password)) return false;
        return Objects.equals(userName, ftpDetail.userName);
    }

    @Override
    public int hashCode() {
        int result = ftpAddress != null ? ftpAddress.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? Arrays.hashCode(password) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FTPDetail{" +
                "ftpAddress='" + ftpAddress + '\'' +
                ", port=" + port +
                ", folder='" + folder + '\'' +
                ", userName='" + userName + '\'' +
                ", password=" + new String(password) +
                '}';
    }
}
