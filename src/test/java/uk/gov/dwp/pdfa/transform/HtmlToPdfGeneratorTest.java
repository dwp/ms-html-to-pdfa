package uk.gov.dwp.pdfa.transform;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.gov.dwp.pdfa.exception.PdfaGeneratorException;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HtmlToPdfGeneratorTest {
    private static Map<String, String> defaultFontMap;
    private static String defaultColourProfile;
    private static String htmlFile;

    private HtmlToPdfGenerator instance;

    @BeforeClass
    public static void init() throws IOException {
        defaultColourProfile = Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/colours/sRGB.icm")));
        htmlFile = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/successfulHtml.html")));
    }

    @Before
    public void setup() throws IOException {
        defaultFontMap = new HashMap<>();
        defaultFontMap.put("courier", Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/fonts/courier.ttf"))));
        defaultFontMap.put("arial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/fonts/arial.ttf"))));

        instance = new HtmlToPdfGenerator();
    }

    @Test
    public void successfullyCreatePdfaBasic() throws IOException, PdfaGeneratorException, XmpParsingException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level A", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("A")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void successfullyCreatePdfaBMultiPage() throws IOException, PdfaGeneratorException, XmpParsingException {
        String base64File = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/pageBreaksHtml.html")));
        String base64pdf = instance.createPdfaDocument(base64File, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(6)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level A", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("A")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void failureWithUndefinedFontInHtml() throws IOException {
        try {
            instance.createPdfaDocument(
                    Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/noFontSupplied.html"))),
                    defaultColourProfile,
                    defaultFontMap,
                    PdfRendererBuilder.PdfAConformance.PDFA_1_A);

            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("Index"));
        }
    }

    @Test
    public void incorrectFormatWithBadImageRenderingInHtml() throws IOException, PdfaGeneratorException, XmpParsingException {
        String base64pdf = instance.createPdfaDocument(
                Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/imageFailureHtml.html"))),
                defaultColourProfile,
                defaultFontMap,
                PdfRendererBuilder.PdfAConformance.PDFA_1_A);

        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level A", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("A")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void changeConformanceLevelIsHandled() throws IOException, PdfaGeneratorException, XmpParsingException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_B);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level B", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("B")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void noConformanceLevelIsHandled() throws IOException, PdfaGeneratorException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.NONE);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNull(pdfDoc.getDocumentCatalog().getMetadata());
    }

    @Test
    public void testSuccessWithOverrideFontForArial() throws IOException, PdfaGeneratorException, XmpParsingException {
        defaultFontMap.replace("arial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/fonts/arialbd.ttf"))));

        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level A", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("A")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void testFailureWithBadHtml() throws IOException {
        try {

            instance.createPdfaDocument(
                    Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/badHtmlFile.html"))),
                    defaultColourProfile,
                    defaultFontMap,
                    PdfRendererBuilder.PdfAConformance.PDFA_1_A);
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("Can't load the XML resource"));
        }
    }

    @Test
    public void testFailureWithBadlyNamedFontOverride() throws IOException {
        Map<String, String> fontMap = new HashMap <>();
        fontMap.put("aaarial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/fonts/arialbd.ttf"))));

        try {
            instance.createPdfaDocument(htmlFile, defaultColourProfile, fontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("Index"));
        }
    }

    @Test
    public void testSuccessWithOverrideColourProfile() throws IOException, XmpParsingException, PdfaGeneratorException {
        String colourProfile = Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/colours/sRGB.icm")));

        String base64pdf = instance.createPdfaDocument(htmlFile, colourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
        assertThat("should be conformance level A", xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo("A")));
        assertThat("should be part 1", xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(1)));
    }

    @Test
    public void testFailureWithBadColourProfileOverride() {
        try {
            instance.createPdfaDocument(htmlFile, Base64.getEncoder().encodeToString("i-am-a-colour-profile".getBytes()), defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A);
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), containsString("Invalid ICC Profile Data"));
        }
    }
}
