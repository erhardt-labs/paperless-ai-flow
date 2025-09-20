package consulting.erhardt.paperless_ai_flow.service;

import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.ocr.OcrClient;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfOcrServiceTest {
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private OcrClient ocrClient;
    
    private PdfOcrService pdfOcrService;
    
    private PaperlessDocument testDocument;
    private PipelineConfiguration.PipelineDefinition testPipelineDefinition;
    
    @BeforeEach
    void setUp() {
        pdfOcrService = new PdfOcrService(webClient, ocrClient);
        
        // Create test document
        testDocument = PaperlessDocument.builder()
                .id(589L)
                .title("Test Document")
                .mimeType("application/pdf")
                .pageCount(1)
                .build();
        
        // Create test pipeline definition
        var ocrConfig = PipelineConfiguration.OcrConfiguration.builder()
                .model("openai/gpt-4o")
                .prompt("Test OCR prompt")
                .build();
        
        testPipelineDefinition = PipelineConfiguration.PipelineDefinition.builder()
                .name("test-pipeline")
                .selector(PipelineConfiguration.SelectorConfiguration.builder().build())
                .ocr(ocrConfig)
                .build();
    }
    
    @Test
    void shouldProcessDocumentSuccessfully() throws IOException {
        // Given: Load test PDF from resources
        var testPdfResource = new ClassPathResource("pdfs/five-pager.pdf");
        var testPdfBytes = testPdfResource.getInputStream().readAllBytes();
        
        // Mock WebClient download
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(testPdfBytes));
        
        // Mock OCR client responses for each page
        when(ocrClient.extractTextAsMarkdown(any(BufferedImage.class), anyString(), anyString()))
                .thenReturn(Mono.just("Page content extracted via OCR"));
        
        // When: Process document
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify result
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    assertThat(markdown).contains("## Page");
                    assertThat(markdown).contains("Page content extracted via OCR");
                })
                .verifyComplete();
        
        // Verify download URL was called correctly
        verify(requestHeadersUriSpec).uri("/api/documents/589/download/");
        
        // Verify OCR was called for each page (5 pages in test PDF)
        verify(ocrClient, times(5)).extractTextAsMarkdown(
                any(BufferedImage.class), 
                eq("openai/gpt-4o"), 
                eq("Test OCR prompt")
        );
    }
    
    @Test
    void shouldHandleDownloadError() {
        // Given: Mock download failure
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class))
                .thenReturn(Mono.error(new RuntimeException("Download failed")));
        
        // When: Process document
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify error is propagated
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                        throwable instanceof RuntimeException && 
                        throwable.getMessage().equals("Download failed"))
                .verify();
    }
    
    @Test
    void shouldHandleOcrError() throws IOException {
        // Given: Load test PDF from resources
        var testPdfResource = new ClassPathResource("pdfs/five-pager.pdf");
        var testPdfBytes = testPdfResource.getInputStream().readAllBytes();
        
        // Mock WebClient download
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(testPdfBytes));
        
        // Mock OCR client error
        when(ocrClient.extractTextAsMarkdown(any(BufferedImage.class), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("OCR failed")));
        
        // When: Process document
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify error is propagated
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                        throwable instanceof RuntimeException && 
                        throwable.getMessage().equals("OCR failed"))
                .verify();
    }
    
    @Test
    void shouldConvertPdfToCorrectNumberOfImages() throws IOException {
        // Given: Load test PDF from resources
        var testPdfResource = new ClassPathResource("pdfs/five-pager.pdf");
        var testPdfBytes = testPdfResource.getInputStream().readAllBytes();
        
        // Mock WebClient download
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(testPdfBytes));
        
        // Mock OCR client responses
        when(ocrClient.extractTextAsMarkdown(any(BufferedImage.class), anyString(), anyString()))
                .thenReturn(Mono.just("OCR result"));
        
        // When: Process document
        var result = pdfOcrService.processDocument(testDocument, testPipelineDefinition);
        
        // Then: Verify processing completes
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
        
        // Capture all OCR calls to verify image processing
        var imageCaptor = ArgumentCaptor.forClass(BufferedImage.class);
        verify(ocrClient, times(5)).extractTextAsMarkdown(
                imageCaptor.capture(), 
                anyString(), 
                anyString()
        );
        
        // Verify all captured images are valid BufferedImages
        var capturedImages = imageCaptor.getAllValues();
        assertThat(capturedImages).hasSize(5);
        capturedImages.forEach(image -> {
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isGreaterThan(0);
            assertThat(image.getHeight()).isGreaterThan(0);
        });
    }
}
