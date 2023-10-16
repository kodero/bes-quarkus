package com.corvid.bes.i18n;

import com.corvid.bes.service.GService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class I18nRepository {

    //@Inject
    private Logger log = Logger.getLogger("I18nRepository");

    @Inject
    GService gService;

    public I18nRepository() {

    }

    public List<ResourceBundle> register(Collection<I18nRequest> i18nRequests) throws Exception {
        List<ResourceBundle> createdBundles = new LinkedList<>();
        for (I18nRequest i18nRequest : i18nRequests) {
            createdBundles.add(register(i18nRequest));
        }
        return createdBundles;
    }

    public ResourceBundle register(I18nRequest request) throws Exception {
        //check if the bundle exists with said lang
        String query = "select DISTINCT b from ResourceBundle b join fetch b.resources " +
                "where b.name =:bundleName and " +
                "b.resourceLocale.language = :language and " +
                "b.resourceLocale.namespace = :namespace";

        List<ResourceBundle> bundles = gService.createQuery(query, ResourceBundle.class)
                .setParameter("bundleName", ResourceBundle.DEFAULT_BUNDLE_NAME)
                .setParameter("language", request.getLang())
                .setParameter("namespace", request.getNamespace())
                .getResultList();

        if (bundles.size() > 1) {
            throw new IllegalStateException(" The bundles are in a wrong state , name " + ResourceBundle.DEFAULT_BUNDLE_NAME
                    + " language " + request.getLang() + " and namespace " + request.getNamespace());
        }

        ResourceBundle createdResourceBundle;

        if (bundles.size() == 0) {
            //create the bundle and the request
            log.info(" creating the bundle ");
            Resource newResource = new Resource(request.getTranslation_id(), request.getTranslated_value());
            ResourceLocale newLocale = new ResourceLocale(request.getLang(), request.getNamespace());
            ResourceBundle newResourceBundle = new ResourceBundle(ResourceBundle.DEFAULT_BUNDLE_NAME, newLocale);
            newResourceBundle.addResource(newResource);
            createdResourceBundle = gService.makePersistent(newResourceBundle);
            log.info(" have created the bundle " + createdResourceBundle);
        } else { /* the bundle exists in the database*/
            ResourceBundle existingBundle = bundles.iterator().next();
            log.info("the existing bundle is " + existingBundle);
            if (existingBundle.getResourceLocale().getNamespace().compareTo(request.getNamespace()) == 0) {
                log.info("the namespace exist,so simply create the resource and add it");
                Resource newResource = new Resource(request.getTranslation_id(), request.getTranslated_value());
                //first, in-case there exists an resource with such a key, remove it first
                existingBundle.addUpdateIfAPresent(newResource);
                createdResourceBundle = gService.makePersistent(existingBundle);
                log.info(" the updated bundle is " + createdResourceBundle);
            } else {
                log.info("new namespace");
                ResourceLocale newLocale = new ResourceLocale(request.getLang(), request.getNamespace());
                Resource newResource = new Resource(request.getTranslation_id(), request.getTranslated_value());
                ResourceBundle newResourceBundle = new ResourceBundle(ResourceBundle.DEFAULT_BUNDLE_NAME, newLocale);
                newResourceBundle.addResource(newResource);
                createdResourceBundle = gService.makePersistent(newResourceBundle);
                log.info("created bundle " + createdResourceBundle);

            }

        }
        return createdResourceBundle;

    }

    public List<ResourceBundle> findAllByLang(String lang) {
        String query = "select DISTINCT b from ResourceBundle b join fetch b.resources " +
                "where b.name =:bundleName and " +
                "b.resourceLocale.language = :language ";

        return gService.createQuery(query, ResourceBundle.class)
                .setParameter("bundleName", ResourceBundle.DEFAULT_BUNDLE_NAME)
                .setParameter("language", lang)
                .getResultList();
    }

    public List<ResourceBundle> findAllByLangAndNamespace(String lang, String namespace) {
        String query = "select DISTINCT b from ResourceBundle b join fetch b.resources " +
                "where b.name =:bundleName and " +
                "b.resourceLocale.language = :language and " +
                "b.resourceLocale.namespace = :namespace";
        return gService.createQuery(query, ResourceBundle.class)
                .setParameter("bundleName", ResourceBundle.DEFAULT_BUNDLE_NAME)
                .setParameter("language", lang)
                .setParameter("namespace", namespace)
                .getResultList();

    }

    public List<ResourceBundle> findAllByLangAndNamespaceAndKey(String lang, String namespaceParam, String keyParam) {
        String query = "select DISTINCT b from ResourceBundle b join fetch b.resources as r " +
                "where b.name =:bundleName and " +
                "b.resourceLocale.language = :language and " +
                "b.resourceLocale.namespace = :namespace and " +
                "r.key =:key";
        return gService.createQuery(query, ResourceBundle.class)
                .setParameter("bundleName", ResourceBundle.DEFAULT_BUNDLE_NAME)
                .setParameter("language", lang)
                .setParameter("namespace", namespaceParam)
                .setParameter("key", keyParam)
                .getResultList();
    }

    public void delete(String id) {
        gService.remove(id, ResourceBundle.class);
    }
}
