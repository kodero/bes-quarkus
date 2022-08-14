package com.corvid.bes.i18n;

import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class ResourceLocale implements Serializable {

    @Column(length = 2)
    @Length(max = 2)
    private String language;

    private String namespace;

    public ResourceLocale() {
    }

    public ResourceLocale(String language, String namespace) {
        this.language = language;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLocale)) return false;

        ResourceLocale that = (ResourceLocale) o;

        if (!language.equals(that.language)) return false;
        if (!namespace.equals(that.namespace)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + namespace.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "ResourceLocale{" +
                "language='" + language + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}