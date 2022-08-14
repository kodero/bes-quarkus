package com.corvid.bes.i18n;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
//import javax.xml.bind.annotation.XmlRootElement;

/**
 * Widget for POSTING i18n json
 *
 * @author mokua
 */
///@XmlRootElement
public class I18nRequest {
    @NotNull
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[a-zA-Z]{2}")
    private String lang;

    @NotNull
    @Size(min = 1)
    private String namespace;

    @NotNull
    @Size(min = 1)
    private String translation_id;

    @NotNull
    @Size(min = 1)
    private String translated_value;

    public I18nRequest(String lang, String namespace, String translation_id, String translated_value) {
        this.lang = lang;
        this.namespace = namespace;
        this.translation_id = translation_id;
        this.translated_value = translated_value;
    }

    public I18nRequest() {
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTranslation_id() {
        return translation_id;
    }

    public void setTranslation_id(String translation_id) {
        this.translation_id = translation_id;
    }

    public String getTranslated_value() {
        return translated_value;
    }

    public void setTranslated_value(String translated_value) {
        this.translated_value = translated_value;
    }
}
