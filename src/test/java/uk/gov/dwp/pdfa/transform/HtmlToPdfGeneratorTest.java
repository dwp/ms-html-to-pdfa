package uk.gov.dwp.pdfa.transform;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.impl.VeraPDFMeta;
import com.adobe.xmp.impl.VeraPDFXMPNode;
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
import uk.gov.dwp.pdfa.items.PdfExtendedConstants;

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

@SuppressWarnings("squid:S1192") // string literals allowed
public class HtmlToPdfGeneratorTest {
    private static Map<String, String> defaultFontMap;
    private static String defaultColourProfile;
    private static String htmlFile;

    private HtmlToPdfGenerator instance;

    @BeforeClass
    public static void init() throws IOException {
        defaultColourProfile = Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/colours/sRGB.icm")));
        htmlFile = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/successfulHtml.html")));

        defaultFontMap = new HashMap<>();
        defaultFontMap.put("courier", Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/fonts/courier.ttf"))));
        defaultFontMap.put("arial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(HtmlToPdfGeneratorTest.class.getResourceAsStream("/fonts/arial.ttf"))));

    }

    @Before
    public void setup() {
        instance = new HtmlToPdfGenerator();
    }

    @Test
    public void successfullyCreatePdfaBasic() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
    }

    @Test
    public void successfullyCreateAccessiblePdfUA() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        String accessibleHtml = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/accessible-test.html")));

        String base64pdf = instance.createPdfaDocument(accessibleHtml, defaultColourProfile, defaultFontMap, PdfExtendedConstants.PDF_UA_CONFORMANCE);
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(2)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfExtendedConstants.PDF_UA_CONFORMANCE);
    }

    @Test
    public void failWithNullConformanceLevel() {
        try {
            instance.createPdfaDocument(Base64.getEncoder().encodeToString("bad-conformance".getBytes()), defaultColourProfile, defaultFontMap, null);
            fail("should have failed with null conformance");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), is(equalTo("'null' is not a valid conformance level, aborting")));
        }
    }

    @Test
    public void failWithDuffConformanceLevel() {
        try {
            instance.createPdfaDocument(Base64.getEncoder().encodeToString("bad-conformance".getBytes()), defaultColourProfile, defaultFontMap, "PDF_A99");
            fail("should have failed with unkonwn conformance");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), is(equalTo("'PDF_A99' is not a valid conformance level, aborting")));
        }
    }

    @Test
    public void successfullyCreatePdfaBMultiPage() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        String base64File = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/pageBreaksHtml.html")));
        String base64pdf = instance.createPdfaDocument(base64File, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(6)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
    }

    @Test
    public void failureWithUndefinedFontInHtml() throws IOException {
        try {
            instance.createPdfaDocument(
                    Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/noFontSupplied.html"))),
                    defaultColourProfile,
                    defaultFontMap,
                    PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());

            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("html element requests font-family: 'tahoma'"));
        }
    }

    @Test
    public void incorrectFormatWithBadImageRenderingInHtml() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        String base64pdf = instance.createPdfaDocument(
                Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/imageFailureHtml.html"))),
                defaultColourProfile,
                defaultFontMap,
                PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());

        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
    }

    @Test
    public void changeConformanceLevelIsHandled() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_B.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_B.toString());
    }

    @Test
    public void noConformanceLevelIsHandled() throws IOException, PdfaGeneratorException {
        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.NONE.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNull(pdfDoc.getDocumentCatalog().getMetadata());
    }

    @Test
    public void testSuccessWithOverrideFontForArial() throws IOException, PdfaGeneratorException, XmpParsingException, XMPException {
        defaultFontMap.replace("arial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/fonts/arialbd.ttf"))));

        String base64pdf = instance.createPdfaDocument(htmlFile, defaultColourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
    }

    @Test
    public void testFailureWithBadHtml() throws IOException {
        try {

            instance.createPdfaDocument(
                    Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(new File("src/test/resources/badHtmlFile.html"))),
                    defaultColourProfile,
                    defaultFontMap,
                    PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("Can't load the XML resource"));
        }
    }

    @Test
    public void testFailureWithBadlyNamedFontOverride() throws IOException {
        Map<String, String> fontMap = new HashMap<>();
        fontMap.put("aaarial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/fonts/arialbd.ttf"))));

        try {
            instance.createPdfaDocument(htmlFile, defaultColourProfile, fontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), startsWith("html element requests font-family: 'courier'"));
        }
    }

    @Test
    public void testFailureWithNullFontOverride() {
        try {
            instance.createPdfaDocument(htmlFile, defaultColourProfile, null, PdfRendererBuilder.PdfAConformance.NONE.toString());
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getCause().getClass().getName(), is(equalTo(NullPointerException.class.getName())));
        }
    }

    @Test
    public void testSuccessWithBadlyNamedFontOverrideIsOkOnNone() throws IOException, PdfaGeneratorException {
        Map<String, String> fontMap = new HashMap<>();
        fontMap.put("aaarial", Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/fonts/arialbd.ttf"))));
        instance.createPdfaDocument(htmlFile, defaultColourProfile, fontMap, PdfRendererBuilder.PdfAConformance.NONE.toString());
    }

    @Test
    public void testSuccessWithOverrideColourProfile() throws IOException, XmpParsingException, PdfaGeneratorException, XMPException {
        String colourProfile = Base64.getEncoder().encodeToString(IOUtils.toByteArray(getClass().getResourceAsStream("/colours/sRGB.icm")));

        String base64pdf = instance.createPdfaDocument(htmlFile, colourProfile, defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
        PDDocument pdfDoc = PDDocument.load(Base64.getDecoder().decode(base64pdf));

        assertThat(pdfDoc.getNumberOfPages(), is(equalTo(1)));
        assertNotNull(pdfDoc.getDocumentCatalog().getMetadata());

        validateDocumentConformance(pdfDoc, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
    }

    @Test
    public void testFailureWithBadColourProfileOverride() {
        try {
            instance.createPdfaDocument(htmlFile, Base64.getEncoder().encodeToString("i-am-a-colour-profile".getBytes()), defaultFontMap, PdfRendererBuilder.PdfAConformance.PDFA_1_A.toString());
            fail("should have thrown an error");

        } catch (PdfaGeneratorException e) {
            assertThat(e.getMessage(), containsString("Invalid ICC Profile Data"));
        }
    }

    private void validateDocumentConformance(PDDocument pdfDoc, String conformance) throws IOException, XMPException, XmpParsingException {
        if (conformance.equalsIgnoreCase(PdfExtendedConstants.PDF_UA_CONFORMANCE)) {
            VeraPDFMeta verMeta = VeraPDFMeta.parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
            VeraPDFXMPNode item = verMeta.getProperty("http://www.aiim.org/pdfua/ns/id/", "part");
            assertNotNull("expecting PDFUA conformity", item);

        } else {
            PdfRendererBuilder.PdfAConformance level = PdfRendererBuilder.PdfAConformance.valueOf(conformance);

            XMPMetadata xmpMetadata = new DomXmpParser().parse(pdfDoc.getDocumentCatalog().getMetadata().exportXMPMetadata());
            assertThat(String.format("should be conformance level %s", conformance), xmpMetadata.getPDFIdentificationSchema().getConformance(), is(equalTo(level.getConformanceValue())));
            assertThat(String.format("should be part %d", level.getPart()), xmpMetadata.getPDFIdentificationSchema().getPart(), is(equalTo(level.getPart())));
        }
    }
}
