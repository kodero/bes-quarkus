// package com.corvid.bes.util;

// import com.corvid.bes.model.ModelDocument;
// import com.corvid.bes.rest.BaseEntityResource;
// import com.corvid.bes.util.fileupload.MetaData;
// import com.corvid.bes.util.fileupload.MetadataExtractor;
// import com.corvid.bes.validation.AuthenticationNotRequired;
// import com.corvid.genericdto.util.FileRepositoryManager;
// import com.corvid.genericdto.util.MimeTypes;
// import com.corvid.genericdto.util.StringUtil;
// import com.corvid.genericdto.util.URLManager;
// import com.fasterxml.jackson.core.JsonEncoding;
// import com.fasterxml.jackson.core.JsonFactory;
// import com.fasterxml.jackson.core.JsonGenerator;
// import org.apache.commons.io.IOUtils;
// import org.jboss.resteasy.plugins.providers.multipart.InputPart;
// import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
// import org.jboss.resteasy.util.GenericType;

// import javax.enterprise.context.ApplicationScoped;
// import javax.inject.Inject;
// import javax.naming.NamingException;
// import javax.persistence.TypedQuery;
// import javax.ws.rs.*;
// import javax.ws.rs.core.MediaType;
// import javax.ws.rs.core.Response;
// import javax.ws.rs.core.StreamingOutput;
// import java.io.*;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import java.util.logging.Logger;


// /**
//  * @author mokua
//  */
// @Path("/modelDocuments")
// @ApplicationScoped
// public class ModelDocumentResource extends BaseEntityResource<ModelDocument> {

//     public static final String FILE_NAME = "fileName";

//     public static final String FILE_TITLE = "title";

//     public static final String FILE_DESCRIPTION = "description";

//     public ModelDocumentResource() {
//         super(ModelDocument.class);
//     }

//     Logger log = Logger.getLogger("ModelDocumentResource");

//     @POST
//     @Path("/fileUpload")
//     @AuthenticationNotRequired
//     @Consumes("multipart/form-data")
//     @Produces(MediaType.APPLICATION_JSON)
//     public Response uploadFile(MultipartFormDataInput input) throws IOException, NamingException {
//         Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
//         String fileName = input.getFormDataPart(FILE_NAME, new GenericType<String>() {
//         });

//         String title = input.getFormDataPart(FILE_TITLE, new GenericType<String>() {
//         });
//         String description = input.getFormDataPart(FILE_DESCRIPTION, new GenericType<String>() {
//         });

//         log.info(" the file name " + fileName);
//         log.info(" the multi-part file ");
//         //Get file data to save
//         //LemrPreferences lemrPreferences = (LemrPreferences) prefs.get("Lemr");
//         List<InputPart> inputParts = uploadForm.get("file");
//         ModelDocument modelDocument = null;
//         for (InputPart inputPart : inputParts) {
//             try {
//                 //header for extra processing if required
//                 //MultivaluedMap<String, String> header = inputPart.getHeaders();
//                 // convert the uploaded file to inputstream and write it to disk
//                 InputStream inputStream = inputPart.getBody(InputStream.class, null);
//                 final String fullFilePath = /*lemrPreferences.getFileUploadUrl() +*/
//                         "/home/kodero/uploads";
//                 //fullFilePath  = "file:///" +fullFilePath;
//                 log.info("full full path " + fullFilePath);
//                 //String uriString = URLEncoder.encode(fullFilePath, "UTF-8");
//                 //URI uri = URI.create(uriString);
//                 //log.info("the URI "+uri);
//                 final File file = new File(fullFilePath);
//                 OutputStream out = new FileOutputStream(file);
//                 int read = 0;
//                 byte[] bytes = new byte[2048];
//                 while ((read = inputStream.read(bytes)) != -1) {
//                     out.write(bytes, 0, read);
//                 }
//                 inputStream.close();
//                 out.flush();
//                 out.close();

//                 // Title and description
//                 MetadataExtractor extractor = new MetadataExtractor();
//                 MetaData metadata = extractor.extractMetadata(file);
//                 if (!StringUtil.isDefined(title)) {
//                     if (StringUtil.isDefined(metadata.getTitle())) {
//                         title = metadata.getTitle();
//                     } else {
//                         title = "";
//                     }
//                     if (!StringUtil.isDefined(description) && StringUtil.isDefined(metadata.getSubject())) {
//                         description = metadata.getSubject();
//                     }
//                 }
//                 if (!StringUtil.isDefined(description)) {
//                     description = "";
//                 }

//                 //create the node and put the uploaded document there
//                 modelDocument = new ModelDocument();
//                 modelDocument.setDocLabel(title);
//                 modelDocument.setDocumentFileName(fileName);
//                 modelDocument.setDocumentFileSize(file.length());
//                 modelDocument.setAttachmentPath(fullFilePath);
//                 String mimeType = metadata.getContentType();
//                 modelDocument.setDocumentType(mimeType);
//                 modelDocument.setDescription(description);
//                 String icon = FileRepositoryManager.getFileIcon(false, fileName, false);
//                 modelDocument.setDisplayIcon(icon);
//                 final String uuid = UUID.randomUUID().toString();
//                 modelDocument.setUUID(uuid);
//                 String uri = URLManager.getSimpleURL(URLManager.URL_FILE, modelDocument.getUUID());
//                 modelDocument.setURI(uri);
//                 super.persist(modelDocument);
//                 log.info("ok");

//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }

//         // return the result
//         return modelDocument == null ? Response.ok().build() : Response.ok(makeJson(modelDocument)).build();
//     }

//     private String makeJson(ModelDocument doc) throws IOException {
//         ByteArrayOutputStream buff = new ByteArrayOutputStream();
//         JsonGenerator jsonGen = new JsonFactory().createJsonGenerator(buff, JsonEncoding.UTF8);
//         jsonGen.useDefaultPrettyPrinter();
//         //object
//         jsonGen.writeStartObject();
//         jsonGen.writeStringField("AttachmentPath", doc.getAttachmentPath());
//         jsonGen.writeStringField("ContentType", doc.getDocumentType());
//         jsonGen.writeStringField("Description", doc.getDescription());
//         jsonGen.writeStringField("DisplayIcon", doc.getDisplayIcon());
//         jsonGen.writeStringField("DocumentType", doc.getDocumentType());
//         jsonGen.writeStringField("Filename ", doc.getDocumentFileName());
//         jsonGen.writeStringField("Id ", doc.getUUID());
//         jsonGen.writeStringField("Title ", doc.getDocLabel());
//         jsonGen.writeStringField("URI ", doc.getURI());
//         jsonGen.writeEndObject();
//         jsonGen.flush();
//         return buff.toString(JsonEncoding.UTF8.name());
//     }

//     @GET
//     @Path("/files/{documentUUID}")
//     @Produces(MediaType.APPLICATION_JSON)
//     public Response fileMeta(@PathParam("documentUUID") final String documentUUID) throws IOException {
//         return Response.ok(makeJson(findDocumentById(documentUUID))).build();
//     }

//     @GET
//     @AuthenticationNotRequired
//     @Path("/files/download/{documentUUID}")
//     @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN, MimeTypes.PDF_MIME_TYPE})
//     public Response fileDownload(@PathParam("documentUUID") final String documentUUID) throws IOException {

//         final ModelDocument modelDocument = findDocumentById(documentUUID);
//         //get the file from the file system since we have the path

//         //FileOutputStream stream = new FileOutputStream(modelDocument.getAttachmentPath());
//         final StreamingOutput stream = new StreamingOutput() {
//             @Override
//             public void write(OutputStream output) throws IOException, WebApplicationException {
//                 InputStream in = new FileInputStream(modelDocument.getAttachmentPath());
//                 try {
//                     IOUtils.copy(in, output);
//                 } finally {
//                     IOUtils.closeQuietly(output);
//                     IOUtils.closeQuietly(in);
//                 }
//             }
//         };
//         return Response.ok(stream, modelDocument.getDocumentContentType()) //TODO: set content-type of your file
//                 .header("Content-Disposition", "attachment; filename = " + modelDocument.getDocumentFileName())
//                 .build();
//     }

//     public ModelDocument findDocumentById(final String uuid) {
//         TypedQuery<ModelDocument> q = getEntityManager().createQuery("SELECT  m from ModelDocument m where m.UUID = :uuid ", ModelDocument.class);
//         q.setParameter("uuid", uuid);
//         ModelDocument res = q.getSingleResult();
//         return res;
//     }
// }
