package com.corvid.bes.i18n;

import com.corvid.genericdto.data.gdto.GenericDTO;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * We do languages & translations
 *
 * @author mokua
 */
@Path("/i18n")
@RequestScoped
@Transactional
public class I18nResource {

    public static final String UTF_8 = "UTF-8";

    Logger log = Logger.getLogger("I18nResource");

    @Inject
    private Validator validator;

    @Inject
    private I18nRepository repository;

    @DELETE
    @jakarta.ws.rs.Path("/{id:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public jakarta.ws.rs.core.Response delete(@PathParam("id") String id) {
        repository.delete(id);
        return jakarta.ws.rs.core.Response.ok().build();
    }

    /**
     * Retrieve all the messages from the default bundle (messages) and return in the
     * given language
     *
     * @param lang
     * @return
     */
    @GET
    @Path("/{lang:[a-zA-Z]{2}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLang(@PathParam("lang") final String lang) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLang(lang);
        final String json = makeJson(resources);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private String makeJson(List<ResourceBundle> resources) throws IOException {
        Map<String, Set<ResourceBundle>> byNamespace = new LinkedHashMap<>();
        for (ResourceBundle resourceBundle : resources) {
            final String namespace = resourceBundle.getResourceLocale().getNamespace();
            Set<ResourceBundle> res = byNamespace.get(namespace);
            if (res == null) {
                //we create and add
                res = new LinkedHashSet<>();
                res.add(resourceBundle);
                byNamespace.put(namespace, res);
            } else {
                res.add(resourceBundle);
            }
        }

        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        JsonGenerator jsonGen = new JsonFactory().createJsonGenerator(buff, JsonEncoding.UTF8);
        jsonGen.useDefaultPrettyPrinter();
        jsonGen.writeStartObject();
        for (Map.Entry<String, Set<ResourceBundle>> b : byNamespace.entrySet()) {
            final String namespace = b.getKey();
            for (ResourceBundle rb : b.getValue()) {
                jsonGen.writeFieldName(namespace);
                jsonGen.writeStartObject();
                for (Resource resource : rb.getResources()) {
                    jsonGen.writeStringField(resource.getKey(), resource.getValue());
                }

                jsonGen.writeEndObject();
            }
        }
        jsonGen.writeEndObject();
        jsonGen.flush();

        return buff.toString(UTF_8);
    }


    @GET
    @Path("/{lang:[a-zA-Z]{2}}/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLangAndNamespace(@PathParam("lang") final String lang,
                                               @PathParam("namespace") final String namespaceParam) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLangAndNamespace(lang, namespaceParam);

        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        JsonGenerator jsonGen = new JsonFactory().createJsonGenerator(buff, JsonEncoding.UTF8);
        jsonGen.useDefaultPrettyPrinter();

        for (ResourceBundle b : resources) {
            jsonGen.writeStartObject();
            for (Resource resource : b.getResources()) {
                jsonGen.writeStringField(resource.getKey(), resource.getValue());
            }

            jsonGen.writeEndObject();
        }

        jsonGen.flush();

        final String json = buff.toString(UTF_8);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/{lang:[a-zA-Z]{2}}/{namespace}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLangAndNamespaceAndKey(@PathParam("lang") final String lang,
                                                     @PathParam("namespace") final String namespaceParam,
                                                     @PathParam("key") final String keyParam) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLangAndNamespaceAndKey(lang, namespaceParam, keyParam);
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        JsonGenerator jsonGen = new JsonFactory().createJsonGenerator(buff, JsonEncoding.UTF8);
        jsonGen.useDefaultPrettyPrinter();

        for (ResourceBundle b : resources) {
            jsonGen.writeStartObject();
            for (Resource resource : b.getResources()) {
                jsonGen.writeStringField(resource.getKey(), resource.getValue());
            }

            jsonGen.writeEndObject();

        }

        jsonGen.flush();

        final String json = buff.toString(UTF_8);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    //DTOS
    @GET
    @Path("/dto/{lang:[a-zA-Z]{2}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLangDTO(@PathParam("lang") final String lang) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLang(lang);
        final List<GenericDTO> res = makeDTO(resources);
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private List<GenericDTO> makeDTO(List<ResourceBundle> resources) throws IOException {
        List<GenericDTO> all = new LinkedList<>();
        for (ResourceBundle resourceBundle : resources) {
            all.add(resourceBundle.toDTO());
        }
        return all;
    }

    @GET
    @Path("/dto/{lang:[a-zA-Z]{2}}/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLangAndNamespaceDTO(@PathParam("lang") final String lang,
                                                  @PathParam("namespace") final String namespaceParam) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLangAndNamespace(lang, namespaceParam);
        return Response.ok(makeDTO(resources), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/dto/{lang:[a-zA-Z]{2}}/{namespace}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllForLangAndNamespaceAndKeyDTO(@PathParam("lang") final String lang,
                                                        @PathParam("namespace") final String namespaceParam,
                                                        @PathParam("key") final String keyParam) throws IOException {
        List<ResourceBundle> resources = repository.findAllByLangAndNamespaceAndKey(lang, namespaceParam, keyParam);
        return Response.ok(makeDTO(resources), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createResource(Collection<I18nRequest> i18nRequests) {

        Response.ResponseBuilder builder = null;
        try {
            // Validates i18nRequests using bean validation
            validateResource(i18nRequests);

            List<ResourceBundle> created = repository.register(i18nRequests);
            log.info("****************");
            log.info(" created " + created);
            log.info("****************");

            // Create an "ok" response
            builder = Response.ok(makeJson(created), MediaType.APPLICATION_JSON_TYPE);
        } catch (ConstraintViolationException ce) {
            // Handle bean validation issues
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            // Handle the unique constrain violation
            Map<String, String> responseObj = new HashMap<String, String>();
            responseObj.put("email", "Email taken");
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (Exception e) {
            // Handle generic exceptions
            Map<String, String> responseObj = new HashMap<String, String>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
        }
        return builder.build();
    }


    private void validateResource(Collection<I18nRequest> requests) throws ConstraintViolationException, ValidationException {
        // Create a bean validator and check for issues.
        for (I18nRequest i18nRequest : requests) {
            Set<ConstraintViolation<I18nRequest>> violations = validator.validate(i18nRequest);

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(new HashSet<ConstraintViolation<?>>(violations));
            }

        }
    }

    private Response.ResponseBuilder createViolationResponse(Set<ConstraintViolation<?>> violations) {
        log.fine("Validation completed. violations found: " + violations.size());

        Map<String, String> responseObj = new HashMap<String, String>();

        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
    }
}
