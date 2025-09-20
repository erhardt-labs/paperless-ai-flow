package consulting.erhardt.paperless_ai_flow.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.test.StepVerifier;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OpenAiOcrClientTest {
    
    @Mock
    private OpenAiChatModel openAiChatModel;
    
    private OpenAiOcrClient openAiOcrClient;
    
    @BeforeEach
    void setUp() {
        openAiOcrClient = new OpenAiOcrClient(openAiChatModel);
    }
    
    @Test
    void shouldExtractTextFromImageSuccessfully() {
        // Given: Create a test image
        var testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var testModel = "openai/gpt-4o";
        var testPrompt = "Extract text from this image";
        
        // When: Extract text from image
        var result = openAiOcrClient.extractTextAsMarkdown(testImage, testModel, testPrompt);
        
        // Then: Verify result contains expected content
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    assertThat(markdown).contains("# OCR Result");
                    assertThat(markdown).contains(testModel);
                    assertThat(markdown).contains("100x100 pixels");
                })
                .verifyComplete();
    }
    
    @Test
    void shouldProcessDifferentModelCorrectly() {
        // Given: Create a test image with different model
        var testImage = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        var testModel = "openai/gpt-4o-mini";
        var testPrompt = "Different prompt for OCR";
        
        // When: Extract text from image
        var result = openAiOcrClient.extractTextAsMarkdown(testImage, testModel, testPrompt);
        
        // Then: Verify result contains model-specific content
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    assertThat(markdown).contains("# OCR Result");
                    assertThat(markdown).contains("openai/gpt-4o-mini");
                    assertThat(markdown).contains("200x150 pixels");
                })
                .verifyComplete();
    }
    
    @Test
    void shouldHandleImageConversionCorrectly() {
        // Given: Create a test image
        var testImage = new BufferedImage(50, 75, BufferedImage.TYPE_INT_RGB);
        var testModel = "openai/gpt-4o";
        var testPrompt = "Test prompt";
        
        // When: Extract text from image
        var result = openAiOcrClient.extractTextAsMarkdown(testImage, testModel, testPrompt);
        
        // Then: Verify processing completes without error
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    assertThat(markdown).contains("50x75 pixels");
                    assertThat(markdown).contains("File size:");
                })
                .verifyComplete();
    }
    
    @Test
    void shouldIncludePromptInResult() {
        // Given: Create a test image with long prompt
        var testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var testModel = "openai/gpt-4o";
        var testPrompt = "This is a very long prompt that should be truncated in the placeholder result because it exceeds the maximum display length";
        
        // When: Extract text from image
        var result = openAiOcrClient.extractTextAsMarkdown(testImage, testModel, testPrompt);
        
        // Then: Verify prompt is included (truncated)
        StepVerifier.create(result)
                .assertNext(markdown -> {
                    assertThat(markdown).isNotEmpty();
                    assertThat(markdown).contains("The prompt provided was:");
                    assertThat(markdown).contains("This is a very long prompt that should be truncated in the placeholder result because it exceeds");
                })
                .verifyComplete();
    }
}
