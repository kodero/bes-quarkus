package com.corvid.bes.rest;

import com.corvid.bes.callbacks.During;
import com.corvid.bes.callbacks.EntityCallbackClass;
import com.corvid.bes.callbacks.EntityCallbackMethod;
import com.corvid.bes.model.AbstractModelBase;
import com.corvid.bes.model.response.BaseResponse;
import com.corvid.bes.model.response.SuccessResponse;
import com.corvid.bes.model.response.ValidationErrorResponse;
import com.corvid.bes.service.GService;
import com.corvid.bes.util.GenericDTOUtil;
import com.corvid.bes.util.Pager;
import com.corvid.bes.validation.ValidateAt;
import com.corvid.bes.validation.ValidationMethod;
import com.corvid.bes.validation.ValidatorClass;
import com.corvid.genericdto.data.gdto.GenericDTO;
import com.corvid.genericdto.util.LocaleAwareMessageInterpolator;
import com.corvid.genericdto.util.PropertyUtils;
import com.corvid.genericdto.util.ReflectionUtils;
import com.corvid.genericdto.util.Reflections;
import com.corvid.genericdto.util.StackTraceUtil;
import com.corvid.genericdto.util.types.BasicTypeHelperImpl;
import com.corvid.genericdto.util.types.ConversionException;
import com.corvid.genericdto.util.types.ConversionManager;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
//import org.jboss.resteasy.spi.HttpResponse;

/**
 * <p>
 * A number of RESTful services implement GET operations on a particular type of entity. For
 * observing the DRY principle, the generic operations are implemented in the <code>BaseEntityResource</code>
 * class, and the other services can inherit from here.
 * </p>
 * <p/>
 * <p>
 * Subclasses will declare a base path using the JAX-RS {@link javax.ws.rs.Path} annotation, for example:
 * </p>
 * <p/>
 * <pre>
 * <code>
 * &#064;Path("/widgets")
 * public class WidgetService extends BaseEntityResource<Widget> {
 * ...
 * }
 * </code>
 * </pre>
 * <p/>
 * <p>
 * will support the following methods:
 * </p>
 * <p/>
 * <pre>
 * <code>
 *   GET /widgets
 *   GET /widgets/:id
 *   GET /widgets/count
 *   GET /widgets/:id/:collection_name
 * </code>
 * </pre>
 * <p/>
 * <p>
 * Subclasses may specify various criteria for filtering entities when retrieving a list of them, by supporting
 * custom query parameters. Pagination is supported by default through the query parameters <code>first</code>
 * and <code>maxResults</code>.
 * </p>
 * <p/>
 * <p>
 * The class is abstract because it is not intended to be used directly, but subclassed by actual JAX-RS
 * endpoints.
 * </p>
 */
@Transactional
@RequestScoped
public abstract class BaseEntityResource<T extends AbstractModelBase> {

    @PersistenceContext
    EntityManager entityManager;

    @Context
    protected HttpServerResponse response;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpServerRequest req;

    /*@Inject*/
    protected Validator validator;

    @Inject
    protected Event<T> entityEventSrc;

    @Inject
    protected Event<EntityCreatedEvent<T>> entityCreatedEventSrc;

    @Inject
    protected Event<EntityUpdatedEvent<T>> entityUpdatedEventSrc;

    @Inject
    protected Event<EntityDeletedEvent<T>> entityDeletedEventSrc;

    //@Inject
    protected Logger log = Logger.getLogger("BaseEntityResource");

    @Inject
    protected GService gService;

    @Inject
    protected GenericDTOUtil genericDTOUtil;

    protected Class<T> entityClass;

    protected LocaleAwareMessageInterpolator interpolator;

    protected BaseEntityResource() {
    }

    protected BaseEntityResource(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public EntityManager getEntityManager() {
        final Session session = entityManager.unwrap(Session.class);
        session.enableFilter("filterByDeleted");
        return entityManager;
    }

    @PostConstruct
    public void init() {
        log.info("*********** init ****************");
        // Create a bean validator and check for issues.
        interpolator = new LocaleAwareMessageInterpolator();
        //set the headers

        javax.validation.Configuration<?> configuration = Validation.byDefaultProvider().configure();
        this.validator = configuration.messageInterpolator(interpolator).buildValidatorFactory().getValidator();
    }

    @DELETE
    @javax.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String id) {
        Response.ResponseBuilder builder = null;
        T entity = gService.find(id, entityClass);
        try {
            //perform validator validation
            processValidationMethods(entity, ValidateAt.DELETE);
            //entity callbacks
            invokeEntityCallbacks(entity, During.DELETE);
            //now perform remove
            gService.softRemove(entity);
            entityDeletedEventSrc.fire(new EntityDeletedEvent<>(entity));
            builder = Response.ok();
        } catch (ConstraintViolationException ce) {
            ce.printStackTrace();
            // Handle bean validation issues
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            e.printStackTrace();
            // Handle the unique constrain violation
            builder = createValidationResponse(e);
        } catch (InvocationTargetException ite){
            ite.printStackTrace();
            builder = createValidationResponse(ite);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", StackTraceUtil.getStackTrace(e));
            builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
        }
        return builder.build();
    }

    @DELETE
    @javax.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}/collections/{collectionName}/{itemId:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteChild(@PathParam("id") String id, @PathParam("collectionName") String collectionName, @PathParam("itemId") String itemId) {
        Field collectionField = Reflections.getField(entityClass, collectionName);
        if (collectionField == null) {
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "The collection field ' " + collectionName + " ' does not exist on the entity ' " + entityClass.getCanonicalName() + "'");
            return Response.status(Response.Status.BAD_REQUEST).entity(responseObj).build();
        }
        Type type = collectionField.getGenericType();
        Class<AbstractModelBase> clzz = null;
        clzz = getGenericParams(type, clzz);
        //now perform remove
        gService.softRemove(itemId, clzz);
        return Response.ok().build();
    }

    protected Response.ResponseBuilder createValidationResponse(ValidationException e) {
        Response.ResponseBuilder builder;Map<String, String> responseObj = new HashMap<>();
        responseObj.put("Validation error", e.getMessage());
        builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        return builder;
    }

    protected Response.ResponseBuilder createValidationResponse(InvocationTargetException ite) {
        Response.ResponseBuilder builder;Map<String, String> responseObj = new HashMap<>();
        responseObj.put("error", ite.getCause().getMessage());
        builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        return builder;
    }

    private Class<AbstractModelBase> getGenericParams(Type type, Class<AbstractModelBase> clzz) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type arr = pType.getActualTypeArguments()[0];
            clzz = (Class<AbstractModelBase>) arr;
        }
        return clzz;
    }

    /**
     * <p>
     * A method for retrieving all entities of a given type.
     * Supports the query parameters
     * <li>
     * <li><code>pageSize</code> , <code>page</code> for pagination.
     * The defaults are <code>pageSize=10</code> , <code>page=1</code>
     * </li>
     * <li> <code>orderBy</code> for sorting, e.g orderBy=-organizationName&orderBy=-createdAt
     * use a leading <code>-</code> to indicate desc, otherwise default it asc.
     * The default orders by createdAt desc.
     * </li>
     * <li><code>fields</code> e.g invoices?fields=name,createdAt,state, party:name, party:state
     * Restricts the fields returned. To traverse relationships, use party:name syntax
     * </li>
     * </li>
     * <p/>
     * <p/>
     * <p/>
     * </p>
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<GenericDTO> getAll(@Context UriInfo uriInfo,
                                   @QueryParam("pageSize") @DefaultValue("25") int pageSize,
                                   @QueryParam("page") @DefaultValue("1") int pageNum,
                                   @QueryParam("orderBy") List<String> orderBy,
                                   @QueryParam("fields") String fieldList,
                                   @QueryParam("where") List<String> where) {
        int numOfRecords = count(where);
        final Pager pager = new Pager(pageSize, pageNum, numOfRecords);
        pager.setNumOfRecords(numOfRecords);
        log.info(" pager " + pager);
        log.info(" order by " + orderBy);
        log.info(" fields " + fieldList);
        log.info(" UriInfo#pathParameters " + uriInfo.getPathParameters());
        String[] fields = fieldList == null ? genericDTOUtil.makeDefaultSelectionFields(entityClass) : fieldList.split(",");

        return getAll(pager, orderBy, fields, uriInfo.getQueryParameters(), where);
    }

    public List<GenericDTO> getAll(final Pager pager,
                                   List<String> orderBy,
                                   String[] fields,
                                   MultivaluedMap<String, String> queryParameters,
                                   List<String> where) {

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<T> root = cq.from(entityClass);
        Predicate[] predicates = extractPredicates(where, cb, root);
        List<Selection<?>> selections = getSelections(fields, root);

        log.info("created selections size " + selections.size());
        cq.multiselect(selections).where(predicates);
        List<Order> od = getOrders(orderBy, cb, root, false);
        cq.orderBy(od);
        //execute query
        Query q = getEntityManager().createQuery(cq);
        log.info(" max results " + pager.getIndexEnd() + ", first result " + pager.getIndexBegin());
        q.setMaxResults(pager.getPageSize());
        q.setFirstResult(pager.getIndexBegin());
        List<Tuple> tupleResult = q.getResultList();
        log.info("results obtained , size " + tupleResult.size());
        //format the output
        List<GenericDTO> dtos = Collections.EMPTY_LIST;
        try {
            dtos = genericDTOUtil.getGenericDTOs(fields, tupleResult, entityClass);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File |//TODO| File Templates.
        }

        serializePagingDetails(pager);
        return dtos;
    }

    protected void serializePagingDetails(Pager pager){
        response.putHeader(Pager.TOTAL_COUNT, String.format("%s", pager.getNumOfRecords()));
        response.putHeader(Pager.CURRENT_PAGE, String.format("%s", pager.getPage()));
        response.putHeader(Pager.START, String.format("%s", pager.getIndexBegin() + 1));
        response.putHeader(Pager.END, String.format("%s", pager.getIndexEnd() + 1));
    }

    private List<GenericDTO> getGenericDTOs2(String[] fields, List<Object[]> tupleResult) throws NoSuchFieldException {
        List<GenericDTO> dtos = new ArrayList<>();
        for (Object[] row : tupleResult) {
            //dumpRow(row);
            GenericDTO dto = genericDTOUtil.getGenericDTO(entityClass, fields, row, null);
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Dotting added to orderBy e.g  orderBy=user:login
     * NOTE : its one level deep
     *
     * @param orderBy
     * @param cb
     * @param root
     * @return
     */
    private List<Order> getOrders(List<String> orderBy, CriteriaBuilder cb, Root<T> root, Boolean excludeDefaultOrder) {
        List<String> _orderBy = new ArrayList<>(orderBy);//make a copy
        if(!excludeDefaultOrder) {
            _orderBy.add("createdAt");//add this to preserve the insertion order
        }
        List<Order> od = new ArrayList<>(_orderBy.size());
        for (String orderDesc : _orderBy) {
            log.info("got order------------------------" + orderDesc);
            if (orderDesc.contains(":")) {
                String[] rels = orderDesc.split(":");
                //root.get(rels[0]).get(rels[1]);
                od.add(rels[0].startsWith("-") ? cb.desc(root.get(rels[0].substring(1)).get(rels[1])) :
                        cb.asc(root.get(rels[0]).get(rels[1])));
            } else {
                od.add(orderDesc.startsWith("-") ? cb.desc(root.get(orderDesc.substring(1))) : cb.asc(root.get(orderDesc)));
            }
        }
        return od;
    }

    protected List<Selection<?>> getSelections(String[] fields, Root<T> root) {
        log.info(" the fields in selection " + Arrays.asList(fields) + " number of fields " + fields.length);
        List<Selection<?>> selections = new ArrayList<>(fields.length);
        Map<String, Join<Object, Object>> existingJoins = new HashMap<>();
        for (String field : fields) {
            log.info("field name " + field);
            if (field.contains(":")) {
                //joins
                //wfTask:owner<firstName;lastName>
                String[] rels = field.split(":");
                Join<Object, Object> currentJoin = existingJoins.containsKey(rels[0])? existingJoins.get(rels[0]) : root.join(rels[0], JoinType.LEFT);
                existingJoins.put(rels[0], currentJoin);
                String relationRest = rels[1].trim();
                if (relationRest.contains("<")) {
                    //owner<firstName;lastName>
                    final int startIndex = relationRest.indexOf("<");
                    final int endIndex = relationRest.indexOf(">");
                    String[] attributes = relationRest.substring(startIndex + 1, endIndex).split(";");
                    String relationName = relationRest.substring(0, startIndex);
                    //TODO specify attributes in the join query
                    log.info(" relation name " + relationName + " attributes " + Arrays.asList(attributes));
                    Path<Object> p = currentJoin.join(relationName, JoinType.LEFT);
                    //selections.add(p);
                    if(attributes.length == 0) {
                        String[] a2 = {"id"}; //re-initialize the array just with the id
                        attributes = a2;
                    }
                    for (String attributeName : attributes) {
                        if (attributeName.contains("[")) {
                            //owner[firstName#lastName]
                            final int si = attributeName.indexOf("["), ei = attributeName.indexOf("]");
                            String[] relationAttrs = attributeName.substring(si + 1, ei).split("#");
                            String relationAttrName = attributeName.substring(0, si);
                            //TODO specify attributes in the join query
                            log.info("Relation name " + relationAttrName + " attributes " + Arrays.asList(relationAttrs));
                            log.info("Current Join " + currentJoin);
                            Join<Object, Object> $join = currentJoin.join(relationName, JoinType.LEFT);
                            Path<Object> $objectPath = $join.join(relationAttrName, JoinType.LEFT);
                            for (String $attributeName : relationAttrs) {
                                selections.add($objectPath.get($attributeName));
                            }
                        }else{
                            selections.add(p.get(attributeName));
                        }
                    }
                } else {
                    Path<Object> p = currentJoin.get(relationRest);
                    selections.add(p);
                }
            } else {
                selections.add(root.get(field));
            }
        }
        return selections;
    }

    public int count(List<String> where) {
        return count(where, entityClass);
    }

    public int count(List<String> where, Class entityClass) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root root = criteriaQuery.from(entityClass);
        criteriaQuery.select(criteriaBuilder.count(root));
        Predicate[] predicates = extractPredicates(where, criteriaBuilder, root);
        criteriaQuery.where(predicates);
        int count = getEntityManager().createQuery(criteriaQuery).getSingleResult().intValue();
        return count;
    }

    /**
     * <p>
     * A method for counting all entities of a given type
     * </p>
     *
     * @param uriInfo application and request context information (see {@see UriInfo} class information for more details)
     * @return
     */
    @GET
    @javax.ws.rs.Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getCount(@Context UriInfo uriInfo, @QueryParam("where") List<String> where) {
        int count  = count(where, entityClass);
        Map<String, Long> result = new HashMap<>();
        result.put("count", new Long(count));
        return result;
    }

    @GET
    @javax.ws.rs.Path("/summaries/{_function}/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, BigDecimal> getSummary(@Context UriInfo uriInfo, @QueryParam("where") List<String> where, @PathParam("_function") String function, @PathParam("field") String field) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = criteriaBuilder.createQuery(BigDecimal.class);
        Root<T> root = criteriaQuery.from(entityClass);
        if("sum".equalsIgnoreCase(function)){
            criteriaQuery.select(criteriaBuilder.sum(root.<BigDecimal>get(field)));
        }else if("min".equalsIgnoreCase(function)){
            criteriaQuery.select(criteriaBuilder.min(root.<BigDecimal>get(field)));
        }else if("max".equalsIgnoreCase(function)){
            criteriaQuery.select(criteriaBuilder.max(root.<BigDecimal>get(field)));
        }else{
            //unsupported function
            throw new UnsupportedOperationException("Operation {"+ function + "} is unsupported!");
        }
        Predicate[] predicates = extractPredicates(where, criteriaBuilder, root);
        criteriaQuery.where(predicates);
        Map<String, BigDecimal> result = new HashMap<>();
        result.put(function, getEntityManager().createQuery(criteriaQuery).getSingleResult());
        return result;
    }

    /**
     * <p>
     * Subclasses may choose to expand the set of supported query parameters (for adding more filtering
     * criteria on search and count) by overriding this method.
     * </p>
     *
     * @param where - the HTTP 'where' query parameter received by the endpoint
     * @param criteriaBuilder - @{link CriteriaBuilder} used by the invoker
     * @param root            @{link Root} used by the invoker
     * @return a list of {@link Predicate}s that will added as query parameters
     */
    protected Predicate[] extractPredicates(final List<String> where, CriteriaBuilder criteriaBuilder, Root<T> root) {

        List<String> whereList = where; //copy into a local variable, don't modify the original
        if (whereList == null || whereList.isEmpty()) {
            log.info(" no where clause ...");
            return new Predicate[]{};
        }

        log.info(" processing where list ' " + whereList + "'");
        List<Predicate> predicates = new ArrayList<>();
        //?[where=<field>,<comparator>,<value>]*
        //?[where=<field>,<comparator>,<value>|<field>,<comparator>,<value>]
        for (String whereParam : whereList) {
            log.info(" the current where clause " + whereParam);
            String[] p = whereParam.split(",");
            log.info(" the fields ' " + Arrays.asList(p) + "' , size of p " + p.length);
            //todo add validations here
            //[where=<field>,<comparator>,<value>|<field>,<comparator>,<value>]
            String[] clauses = whereParam.split("\\|");
            log.info("Where param ; " + whereParam + " has clauses  " + clauses.length);
            int MAX_NO_WHERE_CLAUSE = 16;
            if (clauses.length > MAX_NO_WHERE_CLAUSE) {
                //sanity check prevent denial of service attack
                throw new WebApplicationException("Max number of OR queries for where clause is , ' " + MAX_NO_WHERE_CLAUSE + "'", Response.Status.BAD_REQUEST);
            }
            List<Predicate> allPredicates = new ArrayList<>();
            for (String clause : clauses) {
                List<Predicate> currentPredicates = new ArrayList<>();
                //<field>,<comparator>,<value>
                clause = clause.trim();
                log.info("current clause : "  + clause);
                final String[] split = clause.split(",");
                String[] clauseParts = new String[split.length];
                System.arraycopy(split, 0, clauseParts, 0, clauseParts.length);
                final String field = clauseParts[0];
                convertClausePredicate(criteriaBuilder, root, currentPredicates, clause, clause.split(","), field);
                allPredicates.addAll(currentPredicates);

            }
            Predicate[] pred = new Predicate[allPredicates.size()];
            allPredicates.toArray(pred);
            predicates.add(criteriaBuilder.or(pred));
        }
        log.info(" collected predicates " + predicates);
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    private void convertClausePredicate(CriteriaBuilder criteriaBuilder, Root<T> root, List<Predicate> predicates, String whereParam, String[] p, String fieldPath) {
        if (fieldPath.isEmpty()) {
            throw new WebApplicationException("Invalid path for where request, ' " + fieldPath + "'", Response.Status.BAD_REQUEST);

        }
        //validate path expression
        Path<?> path = getPath(root, fieldPath);
        Class<?> pathClass = path.getJavaType();
        log.info(" the path " + path + " path java type " + path.getJavaType().getName());
        String comparator = p[1];
        ConversionManager conversionManager = ConversionManager.getDefaultManager();
        BasicTypeHelperImpl typeHelper = BasicTypeHelperImpl.getInstance();
        switch (comparator) {
            case "=":
            case "!=":
            case ">":
            case ">=":
            case "<":
            case "<=": {
                //the value must be numeric and field also needs to be numeric
                if (p.length != 3) {
                    throw new WebApplicationException("No value specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                String value = p[2];
                //if("loggedInUser".equalsIgnoreCase(value)) value = userService.currentUser().getId() + ""; //get the real value of the psuedo value loggedInUser
                Object typedValue = null;
                //this also checks compatibility between the path and the input query parameter
                try {
                    typedValue = conversionManager.convertObject(value, pathClass);
                } catch (ConversionException e) {
                    e.printStackTrace();
                    throw new WebApplicationException("The specified value '" + value + "'" +
                            "is incompatible with the target type '" + pathClass.getName() + "', in the request  " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                log.info(" the typed value parameter " + typedValue);
                //pathClass || typedValue must be numeric (since they r the same , we don't need to check both)
                final Class parameterClass = typeHelper.getJavaClass(typedValue);
                log.info(" parameter class " + parameterClass);
                if (typeHelper.isNumericType(parameterClass) || typeHelper.isDateClass(parameterClass)) {
                    switch (comparator) {
                        case "=":
                            predicates.add(criteriaBuilder.equal(path, typedValue));
                            break;
                        case "!=":
                            predicates.add(criteriaBuilder.notEqual(path, typedValue));
                            break;
                        case ">":
                            if (typeHelper.isNumericType(parameterClass)) {
                                predicates.add(criteriaBuilder.gt((Expression<? extends Number>) path, (Number) typedValue));
                            } else if (typeHelper.isDateClass(parameterClass)) {
                                predicates.add(criteriaBuilder.greaterThan((Expression<? extends Comparable>) path, (Comparable) typedValue));
                            }
                            break;
                        case ">=":
                            if (typeHelper.isNumericType(parameterClass)) {
                                predicates.add(criteriaBuilder.ge((Expression<? extends Number>) path, (Number) typedValue));
                            } else if (typeHelper.isDateClass(parameterClass)) {
                                System.out.print("get ======" + new SimpleDateFormat("yyyy-MM-dd").format(typedValue));
                                predicates.add(criteriaBuilder.greaterThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) typedValue));
                            }
                            break;
                        case "<":
                            if (typeHelper.isNumericType(parameterClass)) {
                                predicates.add(criteriaBuilder.lt((Expression<? extends Number>) path, (Number) typedValue));
                            } else if (typeHelper.isDateClass(parameterClass)) {
                                predicates.add(criteriaBuilder.lessThan((Expression<? extends Comparable>) path, (Comparable) typedValue));
                            }
                            break;
                        case "<=":
                            if (typeHelper.isNumericType(parameterClass)) {
                                predicates.add(criteriaBuilder.le((Expression<? extends Number>) path, (Number) typedValue));
                            } else if (typeHelper.isDateClass(parameterClass)) {
                                System.out.print("get =<=====" + new SimpleDateFormat("yyyy-MM-dd").format(typedValue));
                                predicates.add(criteriaBuilder.lessThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) typedValue));
                            }
                            break;
                    }
                } else if (comparator.compareTo("=") == 0 && typeHelper.isEnumType(parameterClass)) {
                    log.info("we have an enum value, class ' " + parameterClass + "'");
                    predicates.add(criteriaBuilder.equal(path, typedValue));
                } else if (comparator.compareTo("!=") == 0 && typeHelper.isEnumType(parameterClass)) {
                    log.info("we have an enum value, class ' " + parameterClass + "'");
                    predicates.add(criteriaBuilder.notEqual(path, typedValue));
                } else if (comparator.compareTo("=") == 0 && typeHelper.isStringType(parameterClass)) {
                    log.info("we have a string value, class ' " + parameterClass + "'");
                    predicates.add(criteriaBuilder.equal(path, typedValue));
                } else if (comparator.compareTo("=") == 0 && typeHelper.isBooleanType(parameterClass)) {
                    log.info("we have a boolean value, class ' " + parameterClass + "'");
                    predicates.add(criteriaBuilder.equal(path, typedValue));
                } else if (comparator.compareTo("!=") == 0 && typeHelper.isBooleanType(parameterClass)) {
                    log.info("we have a boolean value, class ' " + parameterClass + "'");
                    predicates.add(criteriaBuilder.notEqual(path, typedValue));
                } else if (comparator.compareTo("!=") == 0 && typeHelper.isStringType(parameterClass)) {
                  log.info("we have a string value, class ' " + parameterClass + "'");
                  predicates.add(criteriaBuilder.notEqual(path, typedValue));
                } else {
                    throw new WebApplicationException("The specified value '" + value + "' , of type '" + parameterClass + ", " +
                            " type is not Numeric, in the request  " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                break;
            }

            case "LIKE":
            case "NOT_LIKE": {
                log.info(" LIKE comparator , p array => " + Arrays.asList(p));
                if (p.length != 3) {
                    throw new WebApplicationException("No value specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                String value = p[2];
                //path must be of  type Path<String>
                // LIKE & NOT_LIKE  are supported for Strings only
                if (typeHelper.isStringType(pathClass)) {
                    switch (comparator) {
                        case "LIKE":
                            predicates.add(criteriaBuilder.like((Expression<String>) path, "%" + value + "%"));
                            break;
                        case "NOT_LIKE":
                            predicates.add(criteriaBuilder.notLike((Expression<String>) path, value));
                            break;
                    }


                } else {
                    throw new WebApplicationException("LIKE & NOT_LIKE comparator are supported only for String " +
                            "attributes , the path specified,' " + fieldPath + "'  has a type ' " + pathClass + "' ", Response.Status.BAD_REQUEST);
                }
                break;
            }

            //name in ( 'A', 'B', 'C', 'D')
            case "IN":
            case "NOT_IN": {
                log.info(" IN/NOT_IN comparator , p array => " + Arrays.asList(p));
                if (p.length != 3) {
                    throw new WebApplicationException("No value specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                }

                //check the path
                if (typeHelper.isNumericType(pathClass) || typeHelper.isDateClass(pathClass) || typeHelper.isEnumType(pathClass) || typeHelper.isStringType(pathClass)) {
                    String value = p[2];
                    log.info(" IN comparator, value ' " + value + " ' ");
                    int startIndex = value.indexOf("(");
                    int endIndex = value.indexOf(")");
                    String[] values = value.substring(startIndex + 1, endIndex).split(";");
                    if (values.length <= 0) {
                        throw new WebApplicationException("No IN parameter specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                    }
                    List typedValues = new ArrayList();
                    //this also checks compatibility between the path and the input query parameters
                    for (String v : values) {
                        try {
                            typedValues.add(conversionManager.convertObject(v, pathClass));
                        } catch (ConversionException e) {
                            e.printStackTrace();
                            throw new WebApplicationException("The specified value '" + value + "'" +
                                    "is incompatible with the target type '" + pathClass.getName() + "', in the request  " + whereParam + "'", Response.Status.BAD_REQUEST);
                        }
                    }
                    log.info(" the typed value parameters " + typedValues);
                    switch (comparator) {
                        case "IN":
                            predicates.add(path.in(typedValues));
                            break;
                        case "NOT_IN":
                            predicates.add(path.in(typedValues).not());
                            break;
                    }
                } else {
                    throw new WebApplicationException("IN & NOT_IN comparator are supported only for Numeric & Date " +
                            "attributes , the path specified,' " + fieldPath + "'  has a type ' " + pathClass + "' ", Response.Status.BAD_REQUEST);
                }
                break;
            }
            case "IS":
            case "IS_NOT": {
                log.info(" IS/IS_NOT comparator , p array => " + Arrays.asList(p));
                if (p.length != 3) {
                    throw new WebApplicationException("No value specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                String value = p[2];
                if (value.trim().isEmpty()) {
                    throw new WebApplicationException("No parameter specified for where request, ' " + whereParam + "'", Response.Status.BAD_REQUEST);
                }
                log.info(" the value parameter " + value);
                //we support NULL,NOT_NULL,
                switch (value.toUpperCase().trim()) {
                    case "NULL":
                        //in this case, the path/LHS can be any expression
                        predicates.add(criteriaBuilder.isNull(path));
                        break;
                    case "NOT_NULL":
                        predicates.add(criteriaBuilder.isNotNull(path));
                        break;
                    default:
                        //now we have a value ... so proceed as usual ..
                        //note that this works only for Number & Date fields
                        //and its exactly the same as "=" and "!="
                        break;
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid comparator: " + comparator);

        }
    }

    public static Path<?> getPath(Path<?> path, String propertyPath) {
        if (StringUtils.isEmpty(propertyPath))
            return path;

        String name = StringUtils.substringBefore(propertyPath, PropertyUtils.PROPERTY_SEPARATOR);
        Path<?> p = path.get(name);

        return getPath(p, StringUtils.substringAfter(propertyPath, PropertyUtils.PROPERTY_SEPARATOR));

    }

    /**
     * <p>
     * A method for retrieving individual entity instances.
     * </p>
     *
     * @param id entity id
     * @return
     */
    @GET
    @javax.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSingleInstance(@Context UriInfo uriInfo,
                                      @PathParam("id") String id,
                                      @QueryParam("fields") String fieldList,
                                      @QueryParam("collections") String collectionsList) {
        String[] fields = fieldList == null ? genericDTOUtil.makeDefaultSelectionFields(entityClass) : fieldList.split(",");
        List<String> listWithId = new ArrayList<>(fields.length + 1);
        listWithId.addAll(Arrays.asList(fields));
        if(!listWithId.contains("id")) listWithId.add("id");
        //listWithId.add("id"); // NOTE : this is a must .... coz of the collections thingy
        fields = listWithId.toArray(new String[]{});
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<T> root = cq.from(entityClass);
        Predicate condition = cb.equal(root.get("id"), id);
        List<Selection<?>> selections = getSelections(fields, root);
        cq.multiselect(selections).where(condition);
        final TypedQuery<Tuple> query = getEntityManager().createQuery(cq);
        final List<Tuple> tuples = query.getResultList();

        if (tuples.size() == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        GenericDTO dto = genericDTOUtil.getGenericDTO(entityClass, fields, tuples.get(0).toArray(), null);
        T entity = gService.find(id, entityClass);
        log.info("collections param " + collectionsList);
        if (collectionsList != null) {
            //@OneToMany
            //private Collection<GeoCity> geoCities;
            //collections=invoiceLines(x,y,z),foobarz(m,n,o)
            int length = collectionsList.length();
            List<String> colls = new ArrayList<>();
            int start = 0;
            int firstClosingBrace = 0;
            if (collectionsList.contains("(")) {
                for (; firstClosingBrace < length; ) {
                    firstClosingBrace = collectionsList.indexOf(")", start);
                    if (firstClosingBrace < 0) break;
                    firstClosingBrace += 1;
                    System.out.println("closing " + firstClosingBrace);
                    String substring = collectionsList.substring(start, firstClosingBrace);
                    System.out.println(substring);
                    colls.add(substring);
                    start = firstClosingBrace + 1; //the ,
                }
            }
            System.out.println("collection string " + colls + " colls size " + colls.size());
            //try see if we have collectionsOrderBy, something that looks like below
            //collectionsOrderBy=items1|-date,product.name&collectionsOrderBy=items2|name,age
            Map<String, String[]> collectionOrders = new HashMap<>();
            List<String> collectionOrderList = uriInfo.getQueryParameters().get("collectionsOrderBy");
            if (collectionOrderList != null) {
                if(!collectionOrderList.isEmpty()){
                    //now process the order by
                    for(String collectionOrder : collectionOrderList){
                        String[] orderArgs = collectionOrder.split("\\|");
                        log.info("orderArgs : " + Arrays.asList(orderArgs));
                        String collName = orderArgs[0];
                        String[] orderFields = orderArgs[1].split(",");
                        collectionOrders.put(collName, orderFields);
                    }
                }
            }
            log.info("The collection order map : " + collectionOrders);

            String[] collectionFields = colls.toArray(new String[]{});
            log.info("FIELDS " + Arrays.asList(collectionFields) + " fields size " + collectionFields.length);
            for (String collectionField : collectionFields) {
                //invoiceLines(x,y,z)
                String[] childFields = null;
                String relationName = null;
                if (collectionField.contains("(")) {
                    log.info("collectionField " + collectionField);
                    int startIndex = collectionField.indexOf("(");
                    int endIndex = collectionField.lastIndexOf(")");
                    relationName = collectionField.substring(0, startIndex);
                    log.info("RELATION NAME " + relationName);
                    log.info(" START INDEX " + startIndex + " END " + endIndex);
                    final String substring = collectionField.substring(startIndex + 1, endIndex);
                    System.out.println("subs " + substring);
                    //id,index,name,wfTask:owner<firstName;lastName>
                    childFields = substring.substring(0).split(",");
                }
                log.info("RELATION FIELDS " + Arrays.asList(childFields) + " size " + childFields.length);
                //get the relation class
                Field relationField = Reflections.getField(entityClass, relationName);
                //ha, note that fieldType is a generic collection, we need the contained element
                //Collection<GeoCity> ww....
                Type type = relationField.getGenericType();
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) type;
                    Type arr = pType.getActualTypeArguments()[0];
                    Class<?> childClazz = (Class<?>) arr;
                    if (childFields == null) {
                        log.info(" no child collection fields specified, we manufacture default list");
                        childFields = genericDTOUtil.makeDefaultSelectionFields(childClazz);
                        log.info(" manufactured fields " + Arrays.asList(childFields));
                    }
                    //get the name of the corresponding @ManyToOne field on the child
                    //we now proceed to create the query for said child
                    final String parentClassName = entityClass.getSimpleName();
                    final String mappedByName = (relationField.isAnnotationPresent(OneToMany.class))? (relationField.getAnnotation(OneToMany.class)).mappedBy() : null;
                    final String parentIdPath = parentClassName.substring(0, 1).toLowerCase() + parentClassName.substring(1);
                    String parentId = (String) dto.get("id").getValue();
                    log.info(" Child Class " + childClazz.getName() + " , Parent ID Path " + parentIdPath + " parent Id " + parentId + " Child Fields " + Arrays.asList(childFields) + " " +
                            "child fields size " + childFields.length);
                    //check if order fields is present new LinkedList<String>(Arrays.asList(split))
                    String[] orderFields = (collectionOrders.containsKey(relationName)? collectionOrders.get(relationName) : new String[0]);
                    List<GenericDTO> childDTOs = getChild((Class<T>) childClazz, childFields, parentIdPath, entity, orderFields, mappedByName);
                    for (GenericDTO child : childDTOs) {
                        dto.addRelation(relationName, child);
                    }
                } else {
                    //we are cooked...
                    throw new IllegalStateException(" type of collection is wrong ' " + type + "' ");
                }
            }
        }
        return Response.ok(dto).build();
    }

    private List<GenericDTO> getChild(Class<T> childClazz, String[] fields, String parentIdPath, T parent, String[] orderBy, String mappedByName) {
        log.info("getting the children ,  childClazz " + childClazz.getSimpleName() + " fields " + Arrays.asList(fields) + " " +
                " parentIdPath " + parentIdPath + " parent Id " + parent);
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<T> root = cq.from(childClazz);
        List<Selection<?>> selections = getSelections(fields, root);
        log.info("created selections size " + selections.size());
        Predicate condition = null;
        String e = "";
        try{
            condition = cb.equal(root.get(parentIdPath), parent);
        }catch (IllegalArgumentException ex){
            //lets try find the referenced property in the parent
            e = entityClass.getSuperclass().getSimpleName();
            parentIdPath = e.substring(0, 1).toLowerCase() + e.substring(1);
            try {
                condition = cb.equal(root.get(parentIdPath), parent);
            }catch (IllegalArgumentException ex2){
                e = entityClass.getSuperclass().getSuperclass().getSimpleName();
                parentIdPath = e.substring(0, 1).toLowerCase() + e.substring(1);
                try{
                    condition = cb.equal(root.get(parentIdPath), parent);
                }catch (IllegalArgumentException ex3){
                    //try mapped by name
                    if(StringUtils.isNotEmpty(mappedByName)){
                        condition = cb.equal(root.get(mappedByName), parent);
                    }
                }
            }
        }
        cq.multiselect(selections).where(condition);
        List<Order> od = getOrders(new LinkedList<>(Arrays.asList(orderBy)), cb, root, true);
        cq.orderBy(od);
        //execute query
        Query q = getEntityManager().createQuery(cq);
        log.info(" max results " + 100);
        //q.setMaxResults(100);
        List<Tuple> tupleResult = q.getResultList();
        log.info("results obtained , size " + tupleResult.size());
        //format the output
        List<GenericDTO> dtos = Collections.EMPTY_LIST;
        try {
            dtos = genericDTOUtil.getGenericDTOs(fields, tupleResult, childClazz);
        } catch (Exception ex) {
            ex.printStackTrace();  //To change body of catch statement use File |//TODO| File Templates.
        }
        return dtos;
    }

    @GET
    @javax.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}/collections/{collection_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllChildren(@Context UriInfo uriInfo,
                                   @PathParam("id") String id,
                                   @QueryParam("pageSize") @DefaultValue("10") int pageSize,
                                   @QueryParam("page") @DefaultValue("1") int pageNum,
                                   @QueryParam("orderBy") @DefaultValue("id") List<String> orderBy,
                                   @QueryParam("fields") String fieldList,
                                   @PathParam("collection_name") String collectionName,
                                   @QueryParam("where") List<String> where
    ) throws Exception {
        log.info("the id " + id);
        log.info(" order by " + orderBy);
        log.info(" fields " + fieldList);
        log.info("collection name " + collectionName);

        Field collectionField = Reflections.getField(entityClass, collectionName);
        if (collectionField == null) {
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "The collection field ' " + collectionName + " ' does not exist on the entity ' " + entityClass.getName() + "'");
            return Response.status(Response.Status.BAD_REQUEST).entity(responseObj).build();
        }
        String sql = "select e." + collectionName + " from " + entityClass.getSimpleName() + " as e where e.id =:id";
        T entity = gService.find(id, entityClass);
        Type type = collectionField.getGenericType();
        Class<AbstractModelBase> clzz = null;
        clzz = getGenericParams(type, clzz);
        String[] orderFields = new String[orderBy.size()];
        for(int i = 0; i < orderBy.size(); i++){ orderFields[i] = orderBy.get(i); }
        String[] fields = fieldList == null ? genericDTOUtil.makeDefaultSelectionFields(clzz) : fieldList.split(",");
        final String mappedByName = (collectionField.isAnnotationPresent(OneToMany.class))? (collectionField.getAnnotation(OneToMany.class)).mappedBy() : null;
        final String parentIdPath = entityClass.getSimpleName().substring(0, 1).toLowerCase() + entityClass.getSimpleName().substring(1);
        List<GenericDTO> dtos = getChild((Class<T>) clzz, fields, parentIdPath, entity, orderFields, mappedByName);

        int numOfRecords = count(where, clzz);
        final Pager pager = new Pager(pageSize, pageNum, numOfRecords);
        pager.setNumOfRecords(numOfRecords);
        log.info(" pager " + pager);

        serializePagingDetails(pager);
        return Response.ok().entity(dtos).build();
    }

    @POST
    @javax.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateState(@Context HttpHeaders headers,
                                @PathParam("id") String id,
                                GenericDTO entityDTO) {

        List<Locale> acceptedLanguages = headers.getAcceptableLanguages();
        log.info("==> Accepted languages " + acceptedLanguages);
        if ((acceptedLanguages != null) && (!acceptedLanguages.isEmpty())) {
            interpolator.setDefaultLocale(acceptedLanguages.get(0));
        }

        Response.ResponseBuilder builder = null;
        //check if it exists
        log.info("check if the entity exist ' " + entityClass + "'");
        T dbEntity = getEntityManager().find(entityClass, id);
        if (dbEntity == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        log.info("Found the entity with id = '" + dbEntity.getId() + "'");

        try {

            T entity = genericDTOUtil.fromDTO(entityDTO, entityClass);
            log.info(" The  DTO ' " + entityDTO + " ' , entity from DTO ' " + entity + " ' ");
            entity = preUpdate(entity);
            validateEntity(entity);
            extraValidations(entity);

            //process validation methods
            processValidationMethods(entity, ValidateAt.UPDATE);

            //entity callbacks
            invokeEntityCallbacks(entity, During.UPDATE);

            entity = (T) ReflectionUtils.diff(entity, dbEntity);
            entity.setVersion(dbEntity.getVersion());    //set version
            //update the children
            entity = genericDTOUtil.updateChildren(entity, entity.getClass());
            T updatedEntity = getEntityManager().merge(entity);
            Collection<String> fieldNames = GenericDTO.fieldNames(entityDTO);
            fieldNames.add("id");       //add the id field
            builder = Response.ok(URI.create(uriInfo.getAbsolutePath() + "/" + updatedEntity.getId().toString()))
                    .entity(genericDTOUtil.toDTO(updatedEntity, fieldNames.toArray(new String[fieldNames.size()]), entityClass));

            //we raise event
            entityEventSrc.fire(updatedEntity);
            entityUpdatedEventSrc.fire(new EntityUpdatedEvent<>(updatedEntity));
        } catch (ConstraintViolationException ce) {
            ce.printStackTrace();
            // Handle bean validation issues
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            log.info("Validation exception caught!!!!!!!!!!!!!!!!!!!!!!!!!");
            e.printStackTrace();
            // Handle the manual validation exception violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getCause().getMessage());
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (InvocationTargetException ite){
            ite.printStackTrace();
            if(ite.getCause() instanceof ValidationException){
                Map<String, String> responseObj = new HashMap<>();
                responseObj.put("error", ite.getCause().getMessage());
                builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
            }
        }catch (Exception e) {
            e.printStackTrace();
            if(e.getCause() instanceof ValidationException){
                Map<String, String> responseObj = new HashMap<>();
                responseObj.put("error", e.getCause().getMessage());
                builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
            }else {
                // Handle generic exceptions
                Map<String, String> responseObj = new HashMap<>();
                responseObj.put("error", e.getMessage());
                builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
            }
        }
        return builder.build();
    }

    /**
     * Creates a new state from the values provided. Performs validation, and will return a JAX-RS response with either 200 ok,
     * or with a map of fields, and related errors.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createState(@Context HttpHeaders headers, GenericDTO entityDTO) {
        Response.ResponseBuilder builder = null;

        try {
            //make the entity from the dto
            T entity = genericDTOUtil.fromDTO(entityDTO, entityClass);
            dumpEntity(entity);
            entity = preCreate(entity);

            // Validates entity using bean validation
            List<Locale> acceptedLanguages = headers.getAcceptableLanguages();
            log.info("==> Accepted languages " + acceptedLanguages);
            if ((acceptedLanguages != null) && (!acceptedLanguages.isEmpty())) {
                interpolator.setDefaultLocale(acceptedLanguages.get(0));
            }
            validateEntity(entity);

            processValidationMethods(entity, ValidateAt.CREATE);

            //entity callbacks before create
            invokeEntityCallbacks(entity, During.CREATE);

            T createdState = persist(entity);

            log.info(" the PERSISTED entity " + createdState);

            Collection<String> fieldNames = GenericDTO.fieldNames(entityDTO);
            fieldNames.add("id");       //add the id field
            builder = Response.created(URI.create(uriInfo.getAbsolutePath() + "/" + createdState.getId().toString()))
                    .entity(genericDTOUtil.toDTO(createdState, fieldNames.toArray(new String[fieldNames.size()]), entityClass));

            //we raise event
            entityEventSrc.fire(createdState);
            entityCreatedEventSrc.fire(new EntityCreatedEvent<>(createdState));

            //entity callbacks before create
            entityManager.flush();
            invokeEntityCallbacks(entity, During.AFTER_CREATE);
        } catch (ConstraintViolationException ce) {
            ce.printStackTrace();
            // Handle bean validation issues
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            e.printStackTrace();
            // Handle the unique constrain violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("Validation failure", e.getMessage());
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        }catch (InvocationTargetException ite){
            ite.printStackTrace();
            if(ite.getCause() instanceof ValidationException){
                Map<String, String> responseObj = new HashMap<>();
                responseObj.put("error", ite.getCause().getMessage());
                builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info(StackTraceUtil.getStackTrace(e));
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
        }

        return builder.build();
    }

  /**
   * hook for sub-classes
   * @param t
   * @return
   */
  protected T preCreate(T t) {
    return t;
  }

    /**
     * hook for sub-classes
     * @param t
     * @return
     */
    protected T preUpdate(T t) {
        return t;
    }

  /**
   * protected so that the inheriting classes can use
   * @param entity
   * @param validateAt
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
    protected void processValidationMethods(T entity, ValidateAt validateAt) throws IllegalAccessException, InvocationTargetException {
        //business logic validation
        List<Method> validationMethods = Reflections.getMethodsAnnotatedWith(entityClass, ValidationMethod.class);
        log.info("Invoking validation methods on the entity '" + entityClass + "', validateAt : " + validateAt);
        for(Method m : validationMethods){
            //invoke them methods
            System.out.println("Method " + m.getName());
            Set<ValidateAt> validateAts = new HashSet<>(Arrays.asList(m.getAnnotation(ValidationMethod.class).when()));
            log.info("validateAts : " + validateAts);
            if(m.getAnnotation(ValidationMethod.class).enabled()
                    && (validateAts.contains(validateAt) || validateAts.contains(ValidateAt.ALWAYS))) m.invoke(entity);
        }
        //validator class
        if(entityClass.isAnnotationPresent(ValidatorClass.class)){
            Class validatorClass = (entityClass.getDeclaredAnnotation(ValidatorClass.class)).value();
            log.info("Running validations on the validator class : " + validatorClass);
            Object validator = CDI.current().select(validatorClass).get();
            //Object validator = getBean(validatorClass);
            List<Method> validatorValidationMethods = Reflections.getMethodsAnnotatedWith(validatorClass, ValidationMethod.class);
            for(Method m : validatorValidationMethods){
                //invoke them methods
                Set<ValidateAt> validateAts = new HashSet<>(Arrays.asList(m.getAnnotation(ValidationMethod.class).when()));
                if(m.getAnnotation(ValidationMethod.class).enabled()
                        && (validateAts.contains(validateAt) || validateAts.contains(ValidateAt.ALWAYS))) m.invoke(validator, entity);
            }
        }
    }

    protected void invokeEntityCallbacks(T entity, During when) throws InvocationTargetException, IllegalAccessException {
        //entity callbacks
        List<Method> callbackMethods = Reflections.getMethodsAnnotatedWith(entityClass, EntityCallbackMethod.class);
        log.info("Invoking callback methods on the entity '" + entityClass + "', when : " + when);
        for(Method m : callbackMethods){
            //invoke them methods
            System.out.println("Method " + m.getName());
            Set<During> validateAts = new HashSet<>(Arrays.asList(m.getAnnotation(EntityCallbackMethod.class).when()));
            if(m.getAnnotation(EntityCallbackMethod.class).enabled()
                    && (validateAts.contains(when) || validateAts.contains(During.ALWAYS))) m.invoke(entity);
        }
        //validator class
        if(entityClass.isAnnotationPresent(EntityCallbackClass.class)){
            log.info("(entityClass.getDeclaredAnnotation(EntityCallbackClass.class)) : " + (entityClass.getAnnotation(EntityCallbackClass.class)));
            Class callbackClass = (entityClass.getAnnotation(EntityCallbackClass.class)).value();
            log.info("Running callbacks on the callback class : " + callbackClass);
            Object validator = CDI.current().select(callbackClass).get();
            //Object validator = getBean(validatorClass);
            List<Method> validatorValidationMethods = Reflections.getMethodsAnnotatedWith(callbackClass, EntityCallbackMethod.class);
            for(Method m : validatorValidationMethods){
                //invoke them methods
                Set<During> validateAts = new HashSet<>(Arrays.asList(m.getAnnotation(EntityCallbackMethod.class).when()));
                if(m.getAnnotation(EntityCallbackMethod.class).enabled()
                        && (validateAts.contains(when) || validateAts.contains(ValidateAt.ALWAYS))) m.invoke(validator, entity);
            }
        }
    }


    protected void dumpEntity(T entity) {
        /*XStream xstream = new XStream(new Sun14ReflectionProvider(
                new FieldDictionary(new ImmutableFieldKeySorter())),
                new DomDriver("utf-8"));
        log.info(xstream.toXML(entity));*/

       /* XStream xstream = new XStream(new JsonHierarchicalStreamDriver() {
            public HierarchicalStreamWriter createWriter(Writer writer) {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });

        log.info(xstream.toXML(entity));*/
    }

    /**
     * TODO
     * when u post an entity with @manyToOne relationship,
     * and said children don't exists in the db (i.e don't hav id in the json)
     * ...i need to detect that and persist the children and then the parent
     * and set the ids appropriately.
     *
     * @param entity
     * @return
     * @throws Exception
     */
    protected T persist(T entity) throws Exception {
        log.info(" persist the entity " + entity);
        return gService.makePersistent(entity);
    }


    /**
     * <p>
     * Validates the given Member variable and throws validation exceptions based on the type of error. If the error is standard
     * bean validation errors then it will throw a ConstraintValidationException with the set of the constraints violated.
     * </p>
     * <p>
     * If the error is caused because an existing member with the same email is registered it throws a regular validation
     * exception so that it can be interpreted separately.
     * </p>
     *
     * @param state to be validated
     * @throws ConstraintViolationException
     *          If Bean Validation errors exist
     * @throws ValidationException
     *          If member with the same email already exists
     */
    protected void validateEntity(T state) throws ValidationException {

        Set<ConstraintViolation<T>> violations = validator.validate(state);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<ConstraintViolation<?>>(violations));
        }

    }

    /**
     * <p>
     * @Deprecated(prefer to use @ValidationMethod and @ValidatorClass)
     * Subclasses may choose to add extra validations by overriding this method.
     * </p>
     *
     * @param entity the entity for further validations
     */
    @Deprecated
    protected void extraValidations(T entity) {
        //see subclass
    }

    /**
     * Creates a JAX-RS "Bad Request" response including a map of all violation fields, and their message. This can then be used
     * by clients to show violations.
     *
     * @param constraintViolations A set of violations that needs to be reported
     * @return JAX-RS response containing all violations
     */
    protected Response.ResponseBuilder createViolationResponse(Set<ConstraintViolation<?>> constraintViolations) {
        log.info("Validation completed. violations found: " + constraintViolations.size());

        Map<String, String> violations = new HashMap<>();

        for (ConstraintViolation<?> violation : constraintViolations) {
            violations.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        //return
        BaseResponse response = new ValidationErrorResponse("Validation errors occurred. details for more information", violations);
        return Response.status(response.getHttpStatus()).entity(response);
    }

    /**
     * Creates a JAX-RS generic response success response including a status, and the message. This can then be used
     * by clients to show server response.
     *
     * @param httpStatus The http status response
     * @param message The http status response
     * @return JAX-RS response containing all violations
     */
    protected Response.ResponseBuilder createSuccessResponse(int httpStatus, String message) {
        log.info("Message: " + message);
        return Response.status(httpStatus).entity(new SuccessResponse(httpStatus, 1000, message));
    }

    protected Response.ResponseBuilder createResponse(BaseResponse response) {
        return Response.status(response.getHttpStatus()).entity(response);
    }

    public static class EntityCreatedEvent<T> {
        private final T entity;

        public EntityCreatedEvent(T entity) {
            this.entity = entity;
        }

        public T getEntity() {
            return entity;
        }
    }

    public static class EntityUpdatedEvent<T> {
        private final T entity;

        public EntityUpdatedEvent(T entity) {
            this.entity = entity;
        }

        public T getEntity() {
            return entity;
        }
    }

    public static class EntityDeletedEvent<T> {
        private final T entity;

        public EntityDeletedEvent(T entity) {
            this.entity = entity;
        }

        public T getEntity() {
            return entity;
        }
    }
}
