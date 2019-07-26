package uk.gov.dwp.pdfa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.pdfa.exception.PdfaGeneratorException;
import uk.gov.dwp.pdfa.items.JsonPdfInputItem;
import uk.gov.dwp.pdfa.items.PdfExtendedConstants;
import uk.gov.dwp.pdfa.transform.HtmlToPdfGenerator;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings("squid:S1192") // string literals allowed
@RunWith(MockitoJUnitRunner.class)
public class HtmlToPdfResourceTest {
    private static final String FULL_JSON = "{\"font_map\": {\"tahoma\":\"base64-string\",\"arial\":\"base64-string\"}, \"colour_profile\": \"base64-string\", \"page_html\": \"base64-html\", \"conformance_level\": \"PDFA_1_A\"}";
    private static final String BAD_CONFORMANCE_LEVEL = "{\"font_map\": {\"tahoma\":\"base64-string\",\"arial\":\"base64-string\"}, \"colour_profile\": \"base64-string\", \"page_html\": \"base64-html\", \"conformance_level\": \"BAD_PDF_1\"}";
    private static final String MISSING_CONFORMANCE_LEVEL = "{\"font_map\": {\"tahoma\":\"base64-string\",\"arial\":\"base64-string\"}, \"colour_profile\": \"base64-string\", \"page_html\": \"base64-html\"}";
    private static final String MISSING_COLOUR_PROFILE = "{\"font_map\": {\"tahoma\":\"base64-string\",\"arial\":\"base64-string\"}, \"page_html\": \"base64-html\", \"conformance_level\": \"PDFA_1_A\"}";
    private static final String MISSING_FONT_MAP = "{\"colour_profile\": \"base64-string\", \"page_html\": \"base64-html\", \"conformance_level\": \"PDFA_1_A\"}";

    @Mock
    private HtmlToPdfGenerator htmlToPdfGenerator;

    @Captor
    private ArgumentCaptor<String> colourProfileCapture;

    @Captor
    private ArgumentCaptor<Map<String, String>> fontMapCapture;

    @Captor
    private ArgumentCaptor<String> conformanceLevelCapture;

    @Test
    public void testSuccessWithAllParameters() throws IOException, PdfaGeneratorException {
        JsonPdfInputItem item = new ObjectMapper().readValue(FULL_JSON, JsonPdfInputItem.class);
        byte[] returningPdf = "i-am-a-pdf".getBytes();

        String testPdf = Base64.getEncoder().encodeToString(returningPdf);
        when(htmlToPdfGenerator.createPdfaDocument(eq(item.getHtmlDocument()), eq(item.getColourProfile()), eq(item.getFontMap()), conformanceLevelCapture.capture())).thenReturn(testPdf);

        HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
        Response pdfa = instance.generatePdfDocument(new ObjectMapper().writeValueAsString(item));

        assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
        assertThat(conformanceLevelCapture.getValue(), is(equalTo((item.getConformanceLevel()))));
    }

    @Test
    public void testPDFUAWithMissingInputConformance() throws IOException, PdfaGeneratorException {
        JsonPdfInputItem item = new ObjectMapper().readValue(MISSING_CONFORMANCE_LEVEL, JsonPdfInputItem.class);
        byte[] returningPdf = "i-am-a-pdf".getBytes();

        String testPdf = Base64.getEncoder().encodeToString(returningPdf);
        when(htmlToPdfGenerator.createPdfaDocument(eq(item.getHtmlDocument()), eq(item.getColourProfile()), eq(item.getFontMap()), conformanceLevelCapture.capture())).thenReturn(testPdf);

        HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
        Response pdfa = instance.generatePdfDocument(new ObjectMapper().writeValueAsString(item));

        assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
        assertThat(conformanceLevelCapture.getValue(), is(equalTo(PdfExtendedConstants.PDF_UA_CONFORMANCE)));
    }

    @Test
    public void testRejectionWithInvalidConformance() throws IOException, PdfaGeneratorException {
        when(htmlToPdfGenerator.createPdfaDocument(anyString(), anyString(), anyMap(), anyString())).thenThrow(new PdfaGeneratorException("i-am-exception"));
        HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
        Response pdfa = instance.generatePdfDocument(BAD_CONFORMANCE_LEVEL);

        assertThat(pdfa.getEntity().toString(), containsString("PdfaGeneratorException"));
        assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    public void testPDFAWithMissingInputColourProfile() throws IOException, PdfaGeneratorException {
        JsonPdfInputItem item = new ObjectMapper().readValue(MISSING_COLOUR_PROFILE, JsonPdfInputItem.class);
        byte[] returningPdf = "i-am-a-pdf".getBytes();

        String testPdf = Base64.getEncoder().encodeToString(returningPdf);
        when(htmlToPdfGenerator.createPdfaDocument(eq(item.getHtmlDocument()), colourProfileCapture.capture(), eq(item.getFontMap()), conformanceLevelCapture.capture())).thenReturn(testPdf);

        HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
        Response pdfa = instance.generatePdfDocument(new ObjectMapper().writeValueAsString(item));

        assertNotNull(colourProfileCapture.getValue());
        assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
        assertThat(conformanceLevelCapture.getValue(), is(equalTo(item.getConformanceLevel())));
    }

    @Test
    public void testPDFAWithMissingFontMap() throws IOException, PdfaGeneratorException {
        JsonPdfInputItem item = new ObjectMapper().readValue(MISSING_FONT_MAP, JsonPdfInputItem.class);
        byte[] returningPdf = "i-am-a-pdf".getBytes();

        String testPdf = Base64.getEncoder().encodeToString(returningPdf);
        when(htmlToPdfGenerator.createPdfaDocument(eq(item.getHtmlDocument()), eq(item.getColourProfile()), fontMapCapture.capture(), conformanceLevelCapture.capture())).thenReturn(testPdf);

        HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
        Response pdfa = instance.generatePdfDocument(new ObjectMapper().writeValueAsString(item));

        assertNotNull(fontMapCapture.getValue());
        assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
        assertThat(conformanceLevelCapture.getValue(), is(equalTo(item.getConformanceLevel())));

        assertThat(fontMapCapture.getValue().size(), is(equalTo(2)));
        assertTrue(fontMapCapture.getValue().containsKey("arial"));
    }
}
