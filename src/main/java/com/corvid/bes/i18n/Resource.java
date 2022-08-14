package com.corvid.bes.i18n;

import com.corvid.genericdto.data.gdto.GenericDTO;

import javax.persistence.*;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "keys",
                query = "select r.key from Resource r where " +
                        "r.resourceBundle.name = :bundleName and " +
                        "r.resourceBundle.resourceLocale.language = :language"
        ),
        @NamedQuery(
                name = "value",
                query = "select r.value from Resource r where " +
                        "r.resourceBundle.name = :bundleName and " +
                        "r.resourceBundle.resourceLocale.language = :language and " +
                        "r.key = :key")
})

@org.hibernate.annotations.SQLDelete(
        sql = "UPDATE Resource SET deleted = '1' WHERE id = ? "
)
@Table(name = "resource")
public class Resource {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "_key")
    private String key;

    @Column(name = "_value")
    private String value;

    @ManyToOne(fetch = FetchType.EAGER)
    private ResourceBundle resourceBundle;

    public Resource() {
    }

    public Resource(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Resource(String key, String value, ResourceBundle resourceBundle) {
        this.key = key;
        this.value = value;
        this.resourceBundle = resourceBundle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "value='" + value + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource resource = (Resource) o;

        if (!key.equals(resource.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public GenericDTO toDTO() {
        GenericDTO d = new GenericDTO(Resource.class.getName());
        d.addLong("id", getId());
        d.addString("key", getKey());
        d.addString("value", getValue());
        return d;
    }
}
