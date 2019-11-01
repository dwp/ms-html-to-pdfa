package uk.gov.dwp.pdfa;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.pdfa.items.JsonPdfInputItem;
import uk.gov.dwp.pdfa.items.PdfExtendedConstants;
import uk.gov.dwp.pdfa.transform.HtmlToPdfGenerator;

@Path("/")
public class HtmlToPdfResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlToPdfResource.class.getName());
  private final Map<String, String> defaultArialFont = new HashMap<>();
  private final HtmlToPdfGenerator pdfGenerator;
  private final String defaultColourProfile;

  public HtmlToPdfResource(HtmlToPdfGenerator pdfGenerator) throws IOException {
    this.getDefaultArialFont()
            .put(
                    "courier",
                    Base64.getEncoder()
                            .encodeToString(
                                    IOUtils.toByteArray(getClass().getResourceAsStream(
                                            "/fonts/courier.ttf"))));
    this.getDefaultArialFont()
            .put(
                    "arial",
                    Base64.getEncoder()
                            .encodeToString(
                                    IOUtils.toByteArray(getClass().getResourceAsStream(
                                            "/fonts/arial.ttf"))));
    this.defaultColourProfile
            = Base64.getEncoder()
                    .encodeToString(
                            IOUtils.toByteArray(getClass().getResourceAsStream("/colours/sRGB.icm"))
                    );
    this.pdfGenerator = pdfGenerator;
  }

  @POST
  @Path("/generatePdf")
  public Response generatePdfDocument(String json) {
    Response response;

    try {

      JsonPdfInputItem pdfInputItem = new ObjectMapper().readValue(json, JsonPdfInputItem.class);
      LOGGER.info("successfully serialised input json");

      String base64Pdf = pdfGenerator.createPdfaDocument(
                      pdfInputItem.getHtmlDocument(),
                      Optional.ofNullable(pdfInputItem.getColourProfile())
                              .orElseGet(this::getDefaultColourProfile),
                      Optional.ofNullable(pdfInputItem.getFontMap())
                              .filter(fontMap -> fontMap.size() > 0)
                              .orElseGet(this::getDefaultArialFont),
                      Optional.ofNullable(pdfInputItem.getConformanceLevel())
                              .orElse(PdfExtendedConstants.PDF_UA_CONFORMANCE)
              );

      response = Response.status(HttpStatus.SC_OK).entity(base64Pdf).build();
      LOGGER.info("successfully written encoded pdf to response");

    } catch (JsonParseException | JsonMappingException e) {
      response = Response.status(HttpStatus.SC_BAD_REQUEST)
                      .entity(String.format("Json formatting exception :: %s", e.getMessage()))
                      .build();
      LOGGER.debug(e.getClass().getName(), e);
      LOGGER.error(e.getMessage());

    } catch (IOException e) {
      response = Response.status(HttpStatus.SC_BAD_REQUEST)
                      .entity(String.format("IOException :: %s", e.getMessage()))
                      .build();
      LOGGER.debug(e.getClass().getName(), e);
      LOGGER.error(e.getMessage());

    } catch (Exception e) {
      response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                      .entity(String.format("%s :: %s", e.getClass().getName(), e.getMessage()))
                      .build();
      LOGGER.debug(e.getClass().getName(), e);
      LOGGER.error(e.getMessage());
    }

    return response;
  }

  private Map<String, String> getDefaultArialFont() {
    return defaultArialFont;
  }

  private String getDefaultColourProfile() {
    return defaultColourProfile;
  }
}
