package consulting.erhardt.paperless_ai_flow.service;

import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.ocr.OcrClient;
import consulting.erhardt.paperless_ai_flow.paperless.client.PaperlessApiClient;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Specific test for PDF-to-image conversion using the real five-pager.pdf
 */
@ExtendWith(MockitoExtension.class)
class PdfToImageConversionTest {
    
    @Mock
    private PaperlessApiClient paperlessApiClient;
    
    @Mock
    private OcrClient ocrClient;
    
    private PdfOcrService pdfOcrService;
    private PaperlessDocument testDocument;
    private PipelineConfiguration.PipelineDefinition testPipelineDefinition;
    
    @BeforeEach
    void setUp() {
        pdfOcrService = new PdfOcrService(paperlessApiClient, ocrClient);
        
        testDocument = PaperlessDocument.builder()
                .id(589L)
                .title("Five Pager Test Document")
                .mimeType("application/pdf")
                .pageCount(5)
                .build();
        
        var ocrConfig = PipelineConfiguration.OcrConfiguration.builder()
                .model("openai/gpt-4o")
                .prompt("Test OCR prompt")
                .build();
        
        testPipelineDefinition = PipelineConfiguration.PipelineDefinition.builder()
                .name("pdf-conversion-test")
                .selector(PipelineConfiguration.SelectorConfiguration.builder().build())
                .ocr(ocrConfig)
                .build();
    }
    
    @Test
    void shouldConvertRealFivePagePdfToImages() throws IOException {
        // Given: Load actual five-pager.pdf from test resources
        var testPdfResource = new ClassPathResource("pdfs/five-pager.pdf");
        var testPdfBytes = testPdfResource.getInputStream().readAllBytes();
        
        // Verify the PDF is actually loaded and has substantial content
        assertThat(testPdfBytes).hasSizeGreaterThan(1_000_000); // Should be > 1MB
        
        // Mock PaperlessApiClient download to return the real PDF bytes
        when(paperlessApiClient.downloadPdf(589L)).thenReturn(Mono.just(testPdfBytes));
        
        // Mock OCR client to capture image details for verification
        when(ocrClient.extractText(any(BufferedImage.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    BufferedImage image = invocation.getArgument(0);
                    return Mono.just(String.format("OCR result for image %dx%d", image.getWidth(), image.getHeight()));
                });
        
        // When: Process the document (this will trigger PDF-to-image conversion)
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify successful processing with real PDF conversion
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    
                    // Should have content for all 5 pages
                    assertThat(markdown).contains("## Page 1");
                    assertThat(markdown).contains("## Page 2");
                    assertThat(markdown).contains("## Page 3");
                    assertThat(markdown).contains("## Page 4");
                    assertThat(markdown).contains("## Page 5");
                    
                    // Should contain actual image dimension information
                    assertThat(markdown).contains("OCR result for image");
                    
                    // Count page headers to verify all pages were processed
                    var pageCount = markdown.split("## Page").length - 1;
                    assertThat(pageCount).isEqualTo(5);
                })
                .verifyComplete();
        
        // Verify download was called correctly
        verify(paperlessApiClient).downloadPdf(589L);
        
        // Verify OCR was called exactly 5 times (once per page)
        verify(ocrClient, times(5)).extractText(
                any(BufferedImage.class), 
                eq("openai/gpt-4o"), 
                eq("Test OCR prompt")
        );
        
        // Verify each image has reasonable dimensions (PDFs rendered at 300 DPI should be substantial)
        verify(ocrClient, times(5)).extractText(
                argThat(image -> image.getWidth() > 1000 && image.getHeight() > 1000), 
                anyString(), 
                anyString()
        );
    }
    
    @Test 
    void shouldHandleMultiPagePdfWithCorrectPageNumbers() throws IOException {
        // Given: Load the test PDF
        var testPdfResource = new ClassPathResource("pdfs/five-pager.pdf");
        var testPdfBytes = testPdfResource.getInputStream().readAllBytes();
        
        // Mock PaperlessApiClient
        when(paperlessApiClient.downloadPdf(589L)).thenReturn(Mono.just(testPdfBytes));
        
        // Mock OCR to return page-specific content
        when(ocrClient.extractText(any(BufferedImage.class), anyString(), anyString()))
                .thenReturn(Mono.just("Page content from PDF conversion"));
        
        // When: Process document
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify page structure
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    // Verify proper page numbering and structure
                    assertThat(markdown).matches("(?s).*## Page 1.*## Page 2.*## Page 3.*## Page 4.*## Page 5.*");
                    
                    // Each page should have content
                    for (int i = 1; i <= 5; i++) {
                        assertThat(markdown).contains("## Page " + i);
                        assertThat(markdown).contains("Page content from PDF conversion");
                    }
                })
                .verifyComplete();
    }
}
