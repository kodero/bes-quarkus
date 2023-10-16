package com.corvid.bes.i18n;

import com.corvid.genericdto.data.gdto.GenericDTO;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@org.hibernate.annotations.SQLDelete(
        sql = "UPDATE resource_bundle SET deleted = '1' WHERE id = ? "
)
@Table(name = "resource_bundle")
public class ResourceBundle {

    public static final String DEFAULT_BUNDLE_NAME = "messages";

    @Id
    @GeneratedValue
    private Long id;

    private String name;


    @Column(name = "deleted")
    private char deleted = 0;


    private ResourceLocale resourceLocale;

    @OneToMany(mappedBy = "resourceBundle", cascade = {CascadeType.ALL})
    @org.hibernate.annotations.Filter(name = "filterByDeleted")
    private List<Resource> resources;

    public ResourceBundle() {
    }

    public ResourceBundle(String name, ResourceLocale resourceLocale) {
        this.name = name;
        this.resourceLocale = resourceLocale;
        this.resources = new ArrayList<>();
    }

    public ResourceBundle(String name, ResourceLocale resourceLocale, List<Resource> resources) {
        this.name = name;
        this.resourceLocale = resourceLocale;
        this.resources = resources;

    }


    public ResourceBundle addResource(Resource resource) {
        assert resource != null;
        getResources().add(resource);
        resource.setResourceBundle(this);
        return this;
    }

    public ResourceBundle addUpdateIfAPresent(Resource resource) {
        assert resource != null;
        for (Resource r : getResources()) {
            if (r.getKey().compareTo(resource.getKey()) == 0) {
                r.setValue(resource.getValue());
            }
        }
        return this;
    }

    public char getDeleted() {
        return deleted;
    }

    public void setDeleted(char deleted) {
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceLocale getResourceLocale() {
        return resourceLocale;
    }

    public void setResourceLocale(ResourceLocale resourceLocale) {
        this.resourceLocale = resourceLocale;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return "ResourceBundle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceLocale=" + resourceLocale +
                ", resources=" + resources +
                '}';
    }

    public GenericDTO toDTO() {
        GenericDTO d = new GenericDTO(ResourceBundle.class.getName());
        d.addLong("id", getId());
        d.addString("name", getName());
        d.addString("language", getResourceLocale().getLanguage());
        d.addString("namespace", getResourceLocale().getNamespace());
        for (Resource resource : getResources()) {
            d.addRelation("resources", resource.toDTO());
        }

        return d;
    }
}