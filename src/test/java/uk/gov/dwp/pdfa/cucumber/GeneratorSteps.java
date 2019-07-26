package uk.gov.dwp.pdfa.cucumber;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.impl.VeraPDFMeta;
import com.adobe.xmp.impl.VeraPDFXMPNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.pdfa.items.PdfExtendedConstants;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GeneratorSteps {
    private static final Logger LOG = LoggerFactory.getLogger(GeneratorSteps.class.getName());
    private String responseString;
    private int responseCode;

    @When("^I post to the service url \"([^\"]*)\" with json constructed from the following source data$")
    public void iPostToTheServiceUrlWithTheFollowingJsonBody(String url, Map<String, String> jsonValues) throws IOException {
        performStandardHttpPostWithBody(url, buildJsonBody(jsonValues));
    }

    @When("^I post to the service url \"([^\"]*)\" with bad json$")
    public void iPostToTheServiceUrlWithTheFollowingJson(String url) throws IOException {
        performStandardHttpPostWithBody(url, "\"bad\":\"json}");
    }

    @Then("^I get an http response of (\\d+)$")
    public void iGetAHttpResponseOf(int expectedStatusCode) {
        assertThat(responseCode, is(equalTo(expectedStatusCode)));
    }

    @And("^the pdf is Base64 encoded and of the type (NONE|PDF_UA|PDFA_1_A|PDFA_1_B|PDFA_2_A|PDFA_2_B|PDFA_3_B|PDFA_3_U)?$")
    public void thePdfIsOfTheTypeAA(String conformanceLevel) throws IOException, XmpParsingException, XMPException {
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(responseString));

        if (conformanceLevel.equalsIgnoreCase(PdfExtendedConstants.PDF_UA_CONFORMANCE)) {
            VeraPDFMeta verMeta = VeraPDFMeta.parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
            VeraPDFXMPNode item = verMeta.getProperty("http://www.aiim.org/pdfua/ns/id/", "part");
            assertNotNull("expecting PDFUA conformity", item);

        } else if (conformanceLevel.equalsIgnoreCase(PdfRendererBuilder.PdfAConformance.NONE.toString())) {
            assertNull(pdfDoc.getDocumentCatalog().getMetadata());

        } else {
            PdfRendererBuilder.PdfAConformance level = PdfRendererBuilder.PdfAConformance.valueOf(conformanceLevel);

            XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
            Assert.assertThat(String.format("should be conformance level %s", conformanceLevel), xmpMetadata.getPDFIdentificationSchema().getConformance(), CoreMatchers.is(equalTo(level.getConformanceValue())));
            Assert.assertThat(String.format("should be part %d", level.getPart()), xmpMetadata.getPDFIdentificationSchema().getPart(), CoreMatchers.is(equalTo(level.getPart())));
        }
    }

    @SuppressWarnings("squid:S3776") // allowed complexity for readability
    private String buildJsonBody(Map<String, String> jsonValues) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        String delimiter = "";
        for (Map.Entry<String, String> jsonKeyValue : jsonValues.entrySet()) {
            if (!jsonKeyValue.getKey().isEmpty()) {
                builder.append(delimiter);
                builder.append(String.format("\"%s\": ", jsonKeyValue.getKey()));

                if (jsonKeyValue.getKey().equalsIgnoreCase("colour_profile") || jsonKeyValue.getKey().equalsIgnoreCase("page_html")) {
                    builder.append(String.format("\"%s\"", Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File(jsonKeyValue.getValue())))));

                } else if (jsonKeyValue.getKey().equalsIgnoreCase("font_map")) {
                    Map<String, String> fontMap = new ObjectMapper().readValue(jsonKeyValue.getValue(), Map.class);
                    int row = 0;
                    builder.append("{");
                    for (Map.Entry<String, String> item : fontMap.entrySet()) {
                        if (++row  > 1) {
                            builder.append(", ");
                        }
                        builder.append(String.format("\"%s\": \"%s\"", item.getKey(), Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File(item.getValue())))));
                    }
                    builder.append("}");

                } else {
                    builder.append(String.format("\"%s\"", jsonKeyValue.getValue()));
                }

                delimiter = ",";
            }
        }

        return builder.append("}").toString();
    }

    private void performStandardHttpPostWithBody(String uri, String body) throws IOException {
        HttpPost httpUriRequest = new HttpPost(uri);
        if (null != body) {
            HttpEntity entity = new StringEntity(body);
            httpUriRequest.setEntity(entity);
        }

        try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(httpUriRequest)) {
            responseString = EntityUtils.toString(response.getEntity());
            responseCode = response.getStatusLine().getStatusCode();
        }

        String urlString = httpUriRequest.getURI().toASCIIString();
        LOG.info("Executed HTTP Post for {} with response {}", urlString, responseCode);
    }
}
