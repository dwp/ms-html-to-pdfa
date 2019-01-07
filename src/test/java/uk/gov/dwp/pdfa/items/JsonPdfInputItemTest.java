package uk.gov.dwp.pdfa.items;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class JsonPdfInputItemTest {
    private static final String FULL_JSON = "{\"font_map\": {\"tahoma\":\"base64-string\",\"arial\":\"base64-string\"}, \"colour_profile\": \"base64-string\", \"page_html\": \"<html></html>\", \"conformance_level\": \"PDFA_1_A\"}";
    private static final String COLOUR_AND_HTML = "{\"colour_profile\": \"base64-string\", \"page_html\": \"<html></html>\"}";
    private static final String HTML_ONLY = "{\"page_html\": \"<html></html>\"}";

    @Test
    public void testFullSerialisationIsSuccessful() throws IOException {
        JsonPdfInputItem instance = new ObjectMapper().readValue(FULL_JSON, JsonPdfInputItem.class);

        assertNotNull(instance.getFontMap());
        assertNotNull(instance.getColourProfile());
        assertNotNull(instance.getHtmlDocument());
        assertThat(instance.getConformanceLevel(), is(equalTo("PDFA_1_A")));

        assertThat(instance.getFontMap().size(), is(equalTo(2)));
    }

    @Test
    public void testMissingFontIsSuccessful() throws IOException {
        JsonPdfInputItem instance = new ObjectMapper().readValue(COLOUR_AND_HTML, JsonPdfInputItem.class);

        assertNull(instance.getFontMap());
        assertNotNull(instance.getColourProfile());
        assertNotNull(instance.getHtmlDocument());
        assertNull(instance.getConformanceLevel());
    }

    @Test
    public void testMissingFontAndColourIsSuccessful() throws IOException {
        JsonPdfInputItem instance = new ObjectMapper().readValue(HTML_ONLY, JsonPdfInputItem.class);

        assertNull(instance.getFontMap());
        assertNull(instance.getColourProfile());
        assertNotNull(instance.getHtmlDocument());
        assertNull(instance.getConformanceLevel());
    }
}