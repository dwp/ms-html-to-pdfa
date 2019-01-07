package uk.gov.dwp.pdfa.cucumber;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @And("^the pdf is Base64 encoded and of the type (NONE|PDFA_1_A|PDFA_1_B|PDFA_2_A|PDFA_2_B|PDFA_3_B|PDFA_3_U)?$")
    public void thePdfIsOfTheTypeAA(String conformanceLevel) throws IOException, XmpParsingException {
        PdfRendererBuilder.PdfAConformance conformance = PdfRendererBuilder.PdfAConformance.valueOf(conformanceLevel);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(responseString));

        if (conformance.equals(PdfRendererBuilder.PdfAConformance.NONE)) {
            assertNull(pdfDoc.getDocumentCatalog().getMetadata());

        } else {
            assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

            XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
            assertThat(String.format("Should be conformance level %s", conformanceLevel), xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo(conformance.getConformanceValue())));
            assertThat(xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(conformance.getPart())));
        }
    }

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
