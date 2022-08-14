package com.corvid.bes.util;

import java.net.URI;
import java.util.Date;

/**
 * @author mokua
 */
public class FileMetadata {
    /**
     * A human-readable description of the file size e.g.  "225.4KB",
     */
    private String size;

    /**
     * A unique identifier for the current revision of a file.
     * This field is the same rev as elsewhere in the API and can be used to detect changes and avoid conflicts.
     * e.g. "35e97029684fe",
     */
    private String rev;

    /**
     * The file size in bytes. e.g. 230783,
     */
    private long bytes;

    /**
     * The last time the file was modified on Lemr
     */
    private Date modified;

    /**
     * this is the modification time set by the client when the file was added to Lemr.
     * Since this time is not verified (the server stores whatever the client sends up),
     * this should only be used for display purposes (such as sorting) and not, for example,
     * to determine if a file has changed or not.
     */
    private Date clientMtime;

    /**
     * Returns the canonical path to the file or directory.e.g. "/Getting_Started.pdf",
     */
    private String path;

    /**
     * The name of the icon used to illustrate the file type in Lemr
     * e.g. "page_white_acrobat"
     */
    private String icon;

    /**
     * The root or top-level folder depending on your access-level
     */
    private String root;

    /**
     * e.g. "application/pdf",
     */
    private String mimeType;

    private java.net.URI URI;

    public FileMetadata() {
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public Date getClientMtime() {
        return clientMtime;
    }

    public void setClientMtime(Date clientMtime) {
        this.clientMtime = clientMtime;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "bytes=" + bytes +
                ", size='" + size + '\'' +
                ", rev='" + rev + '\'' +
                ", modified=" + modified +
                ", clientMtime=" + clientMtime +
                ", path='" + path + '\'' +
                ", icon='" + icon + '\'' +
                ", root='" + root + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", URI=" + URI +
                '}';
    }

    public void setURI(URI URI) {
        this.URI = URI;
    }

    public URI getURI() {
        return URI;
    }
}
