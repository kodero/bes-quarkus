package com.corvid.bes.util;

import com.corvid.bes.model.AbstractModelBase;
import com.corvid.bes.service.GService;
import com.corvid.genericdto.data.gdto.Attribute;
import com.corvid.genericdto.data.gdto.GenericDTO;
import com.corvid.genericdto.data.gdto.types.*;
import com.corvid.genericdto.util.ReflectionUtils;
import com.corvid.genericdto.util.Reflections;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Time;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Default
@ApplicationScoped
@SuppressWarnings("rawtypes")
public class GenericDTOUtil {

    protected Logger log = Logger.getLogger("GenericDTOUtil");

    @Inject
    private GService gService;

    /**
     * Make a  class T from dto
     *
     * @param genericDTO  hydrated gdto from which to make the class
     * @param entityClass the type
     * @param <T>         returned class type
     * @return
     * @throws Exception
     */
    public <T> T fromDTO(GenericDTO genericDTO, Class<T> entityClass) throws Exception {
        //create the fully formed entity T
        T entity = entityClass.newInstance();
        ObjectMapper mapper = new ObjectMapper();
        //attributes
        Set<Map.Entry<String, Attribute>> allAttributes = genericDTO.attributeMap().entrySet();
        log.info("The attributes " + allAttributes);
        for (Map.Entry<String, Attribute> entry : allAttributes) {
            log.info("current field " + entry.getKey());
            final String fieldName = entry.getKey().trim();
            final Attribute attribute = entry.getValue();
            Field targetField = Reflections.getField(entityClass, fieldName);
            Class<?> targetFieldType = targetField.getType();

            if (attribute instanceof DurationType) {
                DurationType v = (DurationType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof CalendarDateType) {
                CalendarDateType v = (CalendarDateType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof MoneyType) {
                MoneyType v = (MoneyType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof BigDecimalType) {
                BigDecimalType v = (BigDecimalType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof BigIntegerType) {
                BigIntegerType v = (BigIntegerType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof BooleanType) {
                BooleanType v = (BooleanType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof DateType) {
                DateType v = (DateType) attribute;
                if (Calendar.class.isAssignableFrom(targetFieldType)) {
                    Calendar calendar = ReflectionUtils.dateToCalendar(v.getValue());
                    Reflections.setAndWrap(targetField, entity, calendar);
                } else if (Date.class.isAssignableFrom(targetFieldType)) {
                    Reflections.setAndWrap(targetField, entity, v.getValue());
                } else if (java.sql.Date.class.isAssignableFrom(targetFieldType)) {
                    java.sql.Date sqlDate = new java.sql.Date(v.getValue().getTime());
                    Reflections.setAndWrap(targetField, entity, sqlDate);
                } else if (Time.class.isAssignableFrom(targetFieldType)) {
                    Time sqlTime = new Time(v.getValue().getTime());
                    Reflections.setAndWrap(targetField, entity, sqlTime);
                } else {
                    throw new IllegalStateException("unknown date field name " + fieldName + "  target type  " + targetFieldType + " value " + attribute.getValue());
                }
            } else if (attribute instanceof DoubleType) {
                DoubleType v = (DoubleType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof FloatType) {
                FloatType v = (FloatType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof IntType) {
                IntType v = (IntType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof LongType) {
                LongType v = (LongType) attribute;
                Reflections.setAndWrap(targetField, entity, v.getValue());
            } else if (attribute instanceof StringType) {
                StringType v = (StringType) attribute;
                String value = v.getValue();
                //could be an enum field
                if (targetFieldType.isEnum() && value != null) {
                    //create the said enum , with the given string, check if its a simple string
                    if (value.matches("^\\{.*\\}")){//object representation of enum
                        Map<String,Object> enumMap = mapper.readValue(value, Map.class);
                        value = (String)enumMap.get("key");
                    }
                    Enum value1 = Enum.valueOf((Class<Enum>) targetFieldType, value);
                    Reflections.setAndWrap(targetField, entity, value1);
                } else {
                    Reflections.setAndWrap(targetField, entity, value);
                }
            } else if (attribute == null) {
                //use m to check for the types as fall back plan :-)
                Reflections.setAndWrap(targetField, entity, null);

            } else {
                throw new IllegalStateException("unknown field type " + targetFieldType + " value is " + entry);
            }
        }

        //ManyToOne
        Set<Map.Entry<String, GenericDTO>> r2 = genericDTO.getRelations2().entrySet();
        log.info("The @ManyToOne attributes " + r2);
        for (Map.Entry<String, GenericDTO> entry : r2) {
            final String key = entry.getKey();
            final GenericDTO val = entry.getValue();
            Field field = Reflections.getField(entityClass, key);
            final Attribute<Object> idAttribute = val.get("id");
            String typeName = field.getType().getCanonicalName();
            if (idAttribute != null ){
                log.info("We have an id attribute " + idAttribute);
                Object idValue = idAttribute.getValue();
                log.info("checking if the entity ' " + val.getName() + "' exists");
                Class<? extends AbstractModelBase> childEntityClass = 
                    (Class<? extends AbstractModelBase>) Thread.currentThread().getContextClassLoader().loadClass(typeName); 
                
                AbstractModelBase childDbEntity = gService.find(idValue, childEntityClass);
                if (childDbEntity == null) {
                    log.info("The entity ' " + childEntityClass + "' with id ' " + idValue + "' does not exist in the db ");
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                log.info("Found the entity with id = ' " + childDbEntity + "'");
                
                Object childEntity = fromDTO(val, childEntityClass);
                log.info("The  Child DTO ' " + val + " ' , entity from DTO ' " + childEntity + " ' ");
                
                if(field.isAnnotationPresent(ManyToOne.class)){
                    //check if we need to diff the associated entity
                    List<CascadeType> cascadeTypes = Arrays.asList(field.getAnnotation(ManyToOne.class).cascade());
                    if(cascadeTypes.contains(CascadeType.MERGE) || cascadeTypes.contains(CascadeType.ALL)){
                        ((AbstractModelBase) childEntity).setVersion(childDbEntity.getVersion());    //set version
                        //entity = (T) ReflectionUtils.diff(entity, dbEntity);
                        updateChildren(childEntity, childEntityClass);
                    }
                }

                //Class<?> fieldType = field.getType();
                log.info("Field " + field.getName() + ", entity " + entity + " , child db entity " + childDbEntity);
                Reflections.setAndWrap(field, entity, childEntity);
            } else {
                //Field field = Reflections.getField(entityClass, key);
                Class<?> fieldType = field.getType();
                //
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if(manyToOne.cascade() != null){
                    Set<CascadeType> cascadeTypes = new HashSet<>(Arrays.asList(manyToOne.cascade()));
                    if(cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.MERGE) || cascadeTypes.contains(CascadeType.PERSIST)){
                        //assume a cascade is needed, so plugin the value object as is
                        Reflections.setAndWrap(field, entity, fromDTO(val, fieldType));
                    }
                }
                else{
                    //no cascade, the object has been nullified
                    Reflections.setAndWrap(field, entity, null); //set null reference if the relation id is not found
                }
            }
        }
        //relations
        Set<Map.Entry<String, Set<GenericDTO>>> allRelations = genericDTO.getRelations().entrySet();
        for (Map.Entry<String, Set<GenericDTO>> entry : allRelations) {
            Field field = Reflections.getField(entityClass, entry.getKey());
            Class<?> fieldType = field.getType();
            Collection targets = new ArrayList();
            log.info("Processing relation: " + field.getName());
            //check if fieldType is a collection
            if (ReflectionUtils.isCollectionField(field)) {
                //get the collection type and then create it and set it
                for (GenericDTO dto : entry.getValue()) {
                    //Note that fieldType is a generic collection, we need the contained element
                    //Collection<GeoCity> ww....
                    Type type = field.getGenericType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType) type;
                        Type arr = pType.getActualTypeArguments()[0];
                        Class<?> clzz = (Class<?>) arr;
                        String mappedByName = (field.isAnnotationPresent(OneToMany.class))? (field.getAnnotation(OneToMany.class)).mappedBy() : null;
                        Object child = fromDTO(dto, clzz);
                        //set the child's parent to be entity
                        child = setChildParent(child, entity, mappedByName);
                        targets.add(child);
                    } else {
                        //we are cooked...
                        throw new IllegalStateException(" type of collection is wrong ' " + type + "' ");
                    }
                }
                Reflections.setAndWrap(field, entity, targets);
            } else {
                //a single instance
                GenericDTO val = entry.getValue().iterator().next();
                Reflections.setAndWrap(field, entity, fromDTO(val, fieldType));
            }
            log.info("Finished processing relation: " + field.getName());
        }
        return entity;
    }

    /**
     * set the child's parent
     *
     * @param child
     * @param parentEntity
     * @param <T>
     * @return
     */
    private <T> Object setChildParent(Object child, T parentEntity, String mappedByName) {
        String parentClassName = parentEntity.getClass().getSimpleName();
        String fieldName = parentClassName.substring(0, 1).toLowerCase() + parentClassName.substring(1);
        Field field = null;
        log.log(Level.INFO, String.format(" ==============================The mappedBy name : %s ======================", mappedByName));

        try{
            field = Reflections.getField(child.getClass(), fieldName);
        }catch (Exception e){
            e.printStackTrace();
            log.log(Level.WARNING, String.format("The field named {%s} cannot be found on class {%s}, checking on the parent class...", fieldName, child.getClass().getName()));
        }
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne == null) {
            throw new IllegalArgumentException(" The field named " + fieldName + " on class " + child.getClass() + " does " +
                    "not have ManyToOne annotation");
        }
        log.info("Field found!! " + field.getName());
        Reflections.setAndWrap(field, child, parentEntity);
        log.info("After setAndWrap: " + Reflections.getAndWrap(field, child));
        return child;
    }

    /**
     * update the children nodes to recent version
     *
     * @param entity
     * @return
     */
    public <T> T updateChildren(T entity, Class<?> class_) {
        List<Field> relationFields = Reflections.getFields(class_, OneToMany.class);
        log.info("[Updating Children] OneToMany fields " + relationFields);
        if (relationFields.size() == 0) return entity;
        for (Field field : relationFields) {
            Collection targets = new ArrayList(relationFields.size());
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                Type arr = pType.getActualTypeArguments()[0];
                Class<?> childClazz = (Class<?>) arr;
                Collection children = (Collection) Reflections.getAndWrap(field, entity);
                if(children != null){
                    log.info(field.getName() + " Children Size: " + children.size());
                    for (Object child : children) {
                        if (child instanceof AbstractModelBase) {
                            AbstractModelBase childEntity = (AbstractModelBase) child;
                            String childId = childEntity.getId();
                            log.info("Relation Field: " + field.getName() + ", Id: " + childId);
                            if (childId != null) {
                                AbstractModelBase dbEntity = (AbstractModelBase) gService.getEntityManager().find(childClazz, childId);
                                log.info("ChildDbEntity :" + dbEntity);
                                if (dbEntity != null) {
                                    ((AbstractModelBase) child).setVersion(dbEntity.getVersion());
                                }
                            }
                            targets.add(child);
                        }
                    }
                }
            }
            if (targets.size() > 0) {
                Reflections.setAndWrap(field, entity, targets);
            }
        }
        return entity;
    }

    public <T> List<GenericDTO> getGenericDTOs(String[] fields, List<Tuple> tupleResult, Class<T> entityClass1) throws NoSuchFieldException {
        List<GenericDTO> dtos = new ArrayList<>();
        for (Tuple t : tupleResult) {
            Object[] row = t.toArray();
            dumpRow(fields, row);
            GenericDTO dto = getGenericDTO(entityClass1, fields, row, null);
            dtos.add(dto);
        }
        return dtos;
    }

    public GenericDTO getGenericDTO(Class entityClass, String[] fields, Object[] row, String serializeAs) {
        log.info(" getGenericDTO , entityClass " + entityClass.getSimpleName() + " , fields " + Arrays.asList(fields) + " the " +
                "values " + Arrays.asList(row));
        GenericDTO dto = new GenericDTO(serializeAs == null? entityClass.getName() : serializeAs);
        int i = 0;

        Map<String, GenericDTO> relationships = new HashMap<>();
        for (String fieldName : fields) {
            boolean alreadyIncreased = false;
            log.info("current index " + i);
            //final String fieldName = fields[i++];
            final Object r = row[i];
            log.info("current field name " + fieldName + ", row : " + r);
            Map<String, GenericDTO> relationships2 = new HashMap<>();
            if (fieldName.contains(":")) {
                //relationship
                String[] rels = fieldName.split(":");
                log.info(" the length of rels " + rels.length);
                String relationName = rels[0].trim();
                Field attrClass = ReflectionUtils.getField(relationName, entityClass);
                //check if the relationship exists
                String relationAttributeName = rels[1].trim();
                boolean relationExists = relationships.containsKey(relationName);
                GenericDTO relatedDto = relationExists? relationships.get(relationName) : new GenericDTO(attrClass.getType().getName());
                if(!relationExists) relationships.put(relationName, relatedDto);
                //add to the relationship
                Class<?> fieldType = attrClass.getType();
                Field ff = ReflectionUtils.getField(rels[1], fieldType);
                if (!rels[1].contains("<")) {
                    ReflectionUtils.extractValueFromFieldToDTO(relatedDto, r, relationAttributeName, ff);
                } else if (rels[1].contains("<") && r != null) {
                    //wfTask:owner<firstName;lastName>
                    String[] childFields = null;
                    String collectionField = rels[1].trim();   //owner<firstName;lastName>
                    log.info("collectionField2 " + collectionField);
                    String ownerRelationName;
                    if (collectionField.contains("<")) {
                        int startIndex = collectionField.indexOf("<");
                        int endIndex = collectionField.lastIndexOf(">");
                        ownerRelationName = collectionField.substring(0, startIndex);
                        log.info("RELATION NAME2 " + ownerRelationName);
                        childFields = collectionField.substring(startIndex + 1, endIndex).split(";");
                    } else {
                        //wfTask:owner
                        ownerRelationName = collectionField;
                        //childFields = makeDefaultSelectionFields(<<owner class>>);
                    }
                    Field ownerField = Reflections.getField(fieldType, ownerRelationName);
                    Class<?> ownerClass = ownerField.getType();
                    log.info("owner class2 " + ownerClass.getName());
                    if (childFields == null) {
                        childFields = makeDefaultSelectionFields(ownerClass);
                    }
                    log.info("RELATION FIELDS2 " + Arrays.asList(childFields));
                    log.info("Counter value b4 loop : " + i);
                    GenericDTO ownerDTO = null;
                    for (String attributeName : childFields) {
                        log.info("Counter value : " + i);

                        String firstNameRelationAttributeName = attributeName.trim();
                        log.info("owner relation name " + ownerRelationName + " fist name RAN " + firstNameRelationAttributeName + ", row : " + row[i]);
                        Field firstNameField = Reflections.getField(ownerClass, firstNameRelationAttributeName);
                        //get the value of the field from the class
                        //Method fistNameGetterMethod = Reflections.getGetterMethod(ownerClass, firstNameRelationAttributeName);
                        //Object firstNameValue = Reflections.invokeAndWrap(fistNameGetterMethod, r);
                        //log.info("the field value " + firstNameValue);
                        log.info("r[i]: " + row[i]);
                        log.info("row[].length : " + row.length);
                        log.info("fields[].length : " + fields.length);
                        if (relationships2.containsKey(ownerRelationName)) {
                            ownerDTO = relationships2.get(ownerRelationName);
                            ReflectionUtils.extractValueFromFieldToDTO(ownerDTO, row[i], firstNameRelationAttributeName, firstNameField);
                        } else {
                            ownerDTO = new GenericDTO(ownerClass.getName());
                            ReflectionUtils.extractValueFromFieldToDTO(ownerDTO, row[i], firstNameRelationAttributeName, firstNameField);
                            relationships2.put(ownerRelationName, ownerDTO);
                        }
                        alreadyIncreased = true;
                        i++;
                    }
                    log.info("the owner dto " + ownerDTO);
                    relatedDto.addRelation2(ownerRelationName, ownerDTO);
                }
            } else {
                Field f = ReflectionUtils.getField(fieldName, entityClass);
                ReflectionUtils.extractValueFromFieldToDTO(dto, r, fieldName, f);
            }
            //add the relationships
            for (Map.Entry<String, GenericDTO> entry : relationships.entrySet()) {
                //dto.addRelation(entry.getKey(), entry.getValue());
                dto.addRelation2(entry.getKey(), entry.getValue());
            }
            if(!alreadyIncreased) i++;
        }
        return dto;
    }

    public <T> GenericDTO toDTO(T entity, Class<T> entityClass) throws Exception {
        return toDTO(entity, ReflectionUtils.primitiveFieldNames(entityClass).toArray(new String[0]), entityClass, null);
    }

    public <T> GenericDTO toDTO(T entity, String[] fieldNames, Class<T> entityClass) throws Exception {
        return toDTO(entity, fieldNames, entityClass, null);
    }

    /**
     * Covert the entity into a dto
     * Optionally, you can specify the fields u need, else pass an empty array.
     * Indicate relations using <code>fieldName:attributeName</code>
     *
     * @param entity
     * @param fieldNames
     * @param entityClass
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> GenericDTO toDTO(T entity, String[] fieldNames, Class<T> entityClass, String serializeAs) throws Exception {
        //create the fully formed dto from T
        log.info("Entity class " + entity);
        //all the field names for entity
        String[] fields = fieldNames.length == 0 ? makeDefaultSelectionFields(entity.getClass()) : fieldNames;
        fields = cleanUpFields(fields);
        log.info("The fields " + Arrays.asList(fields));
        Object[] row = collectFieldValues(fields, entity);
        dumpRow(fields, row);

        GenericDTO dto = getGenericDTO(entityClass, fields, row, serializeAs);
        return dto;
    }

    private void dumpRow(String[] fields, Object[] row) {
        System.out.println("********** DUMPING ROW **************");
        assert fields.length == row.length;
        for (int i = 0; i < fields.length; i++) {
            String ss = " Field name => " + fields[i];
            final Object o = row[i];
            final String x = o == null ? "  <NULL>  " : "  :  " + o + " : type " + o.getClass().getCanonicalName();
            System.out.println(ss + x);
        }
        System.out.println("********** END DUMPING ROW **************");
    }

    /**
     * TODO remove spaces, and also check for any weired character
     *
     * @param fields
     * @return
     */
    private String[] cleanUpFields(String[] fields) {
        List<String> cleaned = new ArrayList<>(fields.length);
        for (String field : fields) {
            cleaned.add(field.trim());
        }
        return cleaned.toArray(new String[fields.length]);
    }

    public String[] makeDefaultSelectionFields(Class<?> entityType) {
        List<String> defaults = ReflectionUtils.primitiveFieldNames(entityType);
        //defaults.add("id");
        log.info("the defaults, in makeDefaultSelectionFields " + defaults);
        String[] fieldNames = new String[defaults.size()];
        defaults.toArray(fieldNames);
        return fieldNames;
    }

    public Object[] collectFieldValues(String[] fieldsNames, Object entity) {
        List<Object> values = new ArrayList<>(fieldsNames.length);
        for (String fieldName : fieldsNames) {
            log.info("Entity : " + entity + " fieldName : " + fieldName);
            Object val = collectFieldValue(entity, fieldName);
            values.add(val);
        }
        return values.toArray(new Object[values.size()]);
    }

    private Object collectFieldValue(Object entity, String fieldName) {
        log.info(" Entity " + entity + " field name " + fieldName);
        Object val;
        if (fieldName.contains(":")) {
            String[] attrs = fieldName.split(":");
            String fn = attrs[0];
            Field relationObjectField = Reflections.getField(entity.getClass(), fn);
            Object relationObjectFieldValue = Reflections.getAndWrap(relationObjectField, entity);
            log.info(" relationObjectFieldValue " + relationObjectFieldValue);
            val = collectFieldValue(relationObjectFieldValue, attrs[1]);
        } else if(entity == null){
            val = null;
        } else{
            log.info("entity class " + entity.getClass());
            final Method getterMethod = Reflections.getGetterMethod(entity.getClass(), fieldName);
            log.info("getter  " + getterMethod);
            val = Reflections.invokeAndWrap(getterMethod, entity);
            log.info(" the val " + val);
        }
        return val;
    }
}
