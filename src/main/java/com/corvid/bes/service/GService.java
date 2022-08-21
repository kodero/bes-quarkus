package com.corvid.bes.service;

import com.corvid.bes.model.AbstractModelBase;
import com.corvid.genericdto.util.LoggingUtil;

import org.hibernate.Session;
import org.jboss.logging.Logger.Level;

//import javax.ejb.Stateless;
import javax.enterprise.context.RequestScoped;
import javax.persistence.*;
import javax.transaction.Transactional;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@Transactional
@RequestScoped
public class GService {

    //@Inject
    @PersistenceContext
    private EntityManager em;

    public <T> T makePersistent(T entity) throws Exception {
        final T merge = em.merge(entity);
        em.flush();
        return merge;
    }

    public <T> T persist(T entity) {
        return em.merge(entity);
    }

    public <T> T edit(T entity) {
        return getEntityManager().merge(entity);
    }

    public <T> void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public <T extends AbstractModelBase> void softRemove(String id, Class<T> entityClass) {
        T entity = find(id, entityClass);
        if (entity == null) {
            throw new EntityNotFoundException(" Entity with id " + id + " does not exist in the database");
        }
        LoggingUtil.log(this.getClass(), Level.DEBUG, String.format(" the entity to be deleted, id %s", entity));

        entity.setDeleted(true);
        //entity.setDeletedAt(new Date());
        getEntityManager().merge(entity);
    }

    public <T extends AbstractModelBase> void softRemove(T entity) {
        entity.setDeleted(true);
        //entity.setDeletedAt(new Date());
        getEntityManager().merge(getEntityManager().merge(entity));
    }

    public <T> void remove(String id, Class<T> entityClass) {
        T entity = find(id, entityClass);
        if (entity == null) {
            throw new EntityNotFoundException(" Entity with id " + id + " does not exist in the database");
        }
        LoggingUtil.log(this.getClass(), Level.DEBUG, String.format("The entity to be deleted, id %s", entity));
        getEntityManager().remove(entity);
    }

    public <T> T find(Object id, Class<T> entityClass) {
        return getEntityManager().find(entityClass, id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> List<T> findAll(Class entityClass) {
        javax.persistence.criteria.CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        return getEntityManager().createQuery(cq).getResultList();
    }

    public Query createNativeQuery(String sql){
        return em.createNativeQuery(sql);
    }

    public Query createQuery(String sql){
        return em.createQuery(sql);
    }

    public <T> TypedQuery<T> createQuery(String sql, Class<T> entityClass){
        return em.createQuery(sql, entityClass);
    }
    public EntityManager getEntityManager() {
        return em;
    }

    public void flush() {
        em.flush();
    }

    public boolean containsField(Field[] fields, String field) {
        return Arrays.asList(fields).stream().anyMatch(x -> x.getName().equalsIgnoreCase(field));
    }

    public EntityManager getFilteredEntityManager(){
        final Session session = em.unwrap(Session.class);
        session.enableFilter("filterByDeleted");
        return em;
    }
}
