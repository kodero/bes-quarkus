package com.corvid.bes.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Filter;

import java.util.Date;

/**
 * @author mokua
 */
@Entity
@Table(name = "model_documents")
@Filter(name = "filterByDeleted")
@NamedQueries({
        @NamedQuery(name = "ModelDocument.findById", query = "SELECT m FROM ModelDocument m WHERE m.id = :id"),
        @NamedQuery(name = "ModelDocument.findByDocumentFileName", query = "SELECT m FROM ModelDocument m WHERE m.documentFileName = :documentFileName")
})
public class ModelDocument extends AbstractModelBase {

    public static final String WEBDAV_FOLDER = "attachments";

    @Size(max = 255)
    @Column(name = "document_file_name")
    private String documentFileName;

    @Size(max = 255)
    @Column(name = "document_content_type")
    private String documentContentType;

    @Column(name = "document_file_size")
    private Long documentFileSize;

    @Column(name = "document_updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date documentUpdatedAt;


    /**
     * the id of the document type of this document
     */
    @Size(max = 255)
    @Column(name = "document_type")
    private String documentType;

    /**
     * doc label
     */
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "doc_label")
    private String docLabel;

    private String attachmentPath;

    private String UUID;

    private String description;

    private String displayIcon;

    private String URI;

    private String nodePath;


    public ModelDocument() {
    }


    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public String getDocumentContentType() {
        return documentContentType;
    }

    public void setDocumentContentType(String documentContentType) {
        this.documentContentType = documentContentType;
    }

    public Long getDocumentFileSize() {
        return documentFileSize;
    }

    public void setDocumentFileSize(Long documentFileSize) {
        this.documentFileSize = documentFileSize;
    }

    public Date getDocumentUpdatedAt() {
        return documentUpdatedAt;
    }

    public void setDocumentUpdatedAt(Date documentUpdatedAt) {
        this.documentUpdatedAt = documentUpdatedAt;
    }


    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }


    public String getDocLabel() {
        return docLabel;
    }

    public void setDocLabel(String docLabel) {
        this.docLabel = docLabel;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ModelDocument)) {
            return false;
        }
        ModelDocument other = (ModelDocument) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ModelDocument[ id=" + id + " ]";
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getDescription() {
        return description;
    }

    public String getUUID() {
        return UUID;
    }

    public String getDisplayIcon() {
        return displayIcon;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public void setDisplayIcon(String displayIcon) {
        this.displayIcon = displayIcon;
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getNodePath() {
        return nodePath;
    }
}
