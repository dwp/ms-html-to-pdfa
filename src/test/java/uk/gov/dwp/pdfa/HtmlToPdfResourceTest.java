package uk.gov.dwp.pdfa;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.pdf.exception.PdfaGeneratorException;
import uk.gov.dwp.pdf.generator.HtmlToPdfGenerator;
import uk.gov.dwp.pdf.generator.PdfConformanceLevel;
import uk.gov.dwp.pdfa.items.JsonPdfInputItem;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings("squid:S1192") // string literals allowed
@RunWith(MockitoJUnitRunner.class)
public class HtmlToPdfResourceTest {
  private static final String FULL_JSON =
      "{\"font_map\": {\"tahoma\":\"YmFzZTY0LWZvbnQ=\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"PDFA_1_A\"}";
  private static final String BAD_CONFORMANCE_LEVEL =
      "{\"font_map\": {\"tahoma\":\"YmFzZTY0LWZvbnQ=\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"BAD_PDF_1\"}";
  private static final String MISSING_CONFORMANCE_LEVEL =
      "{\"font_map\": {\"tahoma\":\"YmFzZTY0LWZvbnQ=\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\"}";
  private static final String MISSING_COLOUR_PROFILE =
      "{\"font_map\": {\"tahoma\":\"YmFzZTY0LWZvbnQ=\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"PDFA_1_A\"}";
  private static final String MISSING_FONT_MAP =
      "{\"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"PDFA_1_A\"}";
  private static final String EMPTY_FONT_MAP =
      "{\"font_map\": {}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"PDFA_1_A\"}";

  @Mock private HtmlToPdfGenerator htmlToPdfGenerator;

  @Captor private ArgumentCaptor<byte[]> colourProfileCapture;

  @Captor private ArgumentCaptor<Map<String, byte[]>> fontMapCapture;

  @Captor private ArgumentCaptor<PdfConformanceLevel> conformanceLevelCapture;

  @Test
  public void testSuccessWithAllParameters() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem item = new ObjectMapper().readValue(FULL_JSON, JsonPdfInputItem.class);
    byte[] returningPdf = "i-am-a-pdf".getBytes();

    when(htmlToPdfGenerator.createPdfDocument(
            eq(item.getHtmlDocument()),
            eq(item.getColourProfile()),
            anyMap(),
            conformanceLevelCapture.capture()))
        .thenReturn(returningPdf);

    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(FULL_JSON);

    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
    assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
    assertThat(
        conformanceLevelCapture.getValue().toString(), is(equalTo(item.getConformanceLevel())));
  }

  @Test
  public void testPDFUAWithMissingInputConformance() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem item =
        new ObjectMapper().readValue(MISSING_CONFORMANCE_LEVEL, JsonPdfInputItem.class);
    byte[] returningPdf = "i-am-a-pdf".getBytes();

    when(htmlToPdfGenerator.createPdfDocument(
            eq(item.getHtmlDocument()),
            eq(item.getColourProfile()),
            anyMap(),
            conformanceLevelCapture.capture()))
        .thenReturn(returningPdf);

    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(MISSING_CONFORMANCE_LEVEL);

    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
    assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
    assertThat(conformanceLevelCapture.getValue(), is(equalTo(PdfConformanceLevel.PDF_UA)));
  }

  @Test
  public void testRejectionWithInvalidConformance() throws IOException, PdfaGeneratorException {
    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(BAD_CONFORMANCE_LEVEL);

    assertThat(pdfa.getEntity().toString(), containsString("IllegalArgumentException"));
    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
  }

  @Test
  public void testPDFAWithMissingInputColourProfile() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem item =
        new ObjectMapper().readValue(MISSING_COLOUR_PROFILE, JsonPdfInputItem.class);
    byte[] returningPdf = "i-am-a-pdf".getBytes();

    when(htmlToPdfGenerator.createPdfDocument(
            eq(item.getHtmlDocument()),
            colourProfileCapture.capture(),
            anyMap(),
            conformanceLevelCapture.capture()))
        .thenReturn(returningPdf);

    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(MISSING_COLOUR_PROFILE);

    assertNotNull(colourProfileCapture.getValue());
    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
    assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
    assertThat(
        conformanceLevelCapture.getValue().toString(), is(equalTo(item.getConformanceLevel())));
  }

  @Test
  public void testPDFAWithMissingFontMap() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem item = new ObjectMapper().readValue(MISSING_FONT_MAP, JsonPdfInputItem.class);
    byte[] returningPdf = "i-am-a-pdf".getBytes();

    when(htmlToPdfGenerator.createPdfDocument(
            eq(item.getHtmlDocument()),
            eq(item.getColourProfile()),
            fontMapCapture.capture(),
            conformanceLevelCapture.capture()))
        .thenReturn(returningPdf);

    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(MISSING_FONT_MAP);

    assertNotNull(fontMapCapture.getValue());
    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
    assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
    assertThat(
        conformanceLevelCapture.getValue().toString(), is(equalTo(item.getConformanceLevel())));

    assertThat(fontMapCapture.getValue().size(), is(equalTo(2)));
    assertTrue(fontMapCapture.getValue().containsKey("arial"));
  }

  @Test
  public void testPDFAWithEmptyFontMap() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem item = new ObjectMapper().readValue(EMPTY_FONT_MAP, JsonPdfInputItem.class);
    byte[] returningPdf = "i-am-a-pdf".getBytes();

    when(htmlToPdfGenerator.createPdfDocument(
            eq(item.getHtmlDocument()),
            eq(item.getColourProfile()),
            fontMapCapture.capture(),
            conformanceLevelCapture.capture()))
            .thenReturn(returningPdf);

    HtmlToPdfResource instance = new HtmlToPdfResource(htmlToPdfGenerator);
    Response pdfa = instance.generatePdfDocument(EMPTY_FONT_MAP);

    assertNotNull(fontMapCapture.getValue());
    assertThat(pdfa.getStatus(), is(equalTo(HttpStatus.SC_OK)));
    assertThat(Base64.getDecoder().decode(pdfa.getEntity().toString()), is(equalTo(returningPdf)));
    assertThat(
            conformanceLevelCapture.getValue().toString(), is(equalTo(item.getConformanceLevel())));

    assertThat(fontMapCapture.getValue().size(), is(equalTo(2)));
    assertTrue(fontMapCapture.getValue().containsKey("arial"));
  }
}
